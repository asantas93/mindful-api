package controllers

import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject

import com.squareup.connect.models.{Order, OrderLineItem}
import org.apache.commons.text.RandomStringGenerator
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents}
import implicits.MoneyLike
import services._

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@Inject
class OrderController @Inject()(
                                 cc: ControllerComponents,
                                 catalogApi: SquareCatalog,
                                 ordersApi: SquareOrders,
                                 transactionsApi: SquareTransactions,
                                 email: Email,
                                 dropbox: Dropbox,
                                 excel: Excel)
  extends AbstractController(cc) {

  def order = Action {
    request =>
      implicit val orderReads: Reads[PublicOrder] = Json.reads[PublicOrder]
      implicit val orderRequestReads: Reads[PublicOrderRequest] = Json.reads[PublicOrderRequest]
      Try {
        request.body.asJson.orElse {
          throw new RuntimeException("Your order could not be understood. Please contact our staff for assistance.")
        } map { _.as[PublicOrderRequest] } foreach {
          request =>
            implicit val order: Order = ordersApi.createOrder(request)
            transactionsApi.completeOrder(order, request)
            tryEmailError { () => logGiftCards(request.orders) }
            tryEmailError { () => email.receiptEmail(request.email, order) }
            tryEmailError { () => request.orders foreach { o => email.giftEmail(o) } }
        }
      } match {
        case Success(_) => Ok { "Order processed." }
        case Failure(e) => BadRequest {
          "Your order resulted in an error and has been cancelled. Please try clearing your browser's cache and " +
            "refreshing this page. If the problem persists please contact our staff with this error message:\n" +
            e.getMessage
        }
      }
  }

  private def tryEmailError(func: () => Unit): Unit = {
    try {
      func()
    } catch {
      case e: Exception => email.genericEmail(
        sys.env("MAINTAINER_EMAIL"),
        s"API Error ${Calendar.getInstance().getTime.toString}",
        s"Encountered error: ${e.toString}\n${e.getMessage}\n${e.getStackTrace.mkString("\n")}"
      )
    }
  }

  def logGiftCards(orders: List[PublicOrder])(implicit squareOrder: Order): Unit = {
    def wrapModifiers(m: String) = if (m.nonEmpty) s"with ($m)" else ""
    val year = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime)
    val logPath = s"/MM/Financial Records/FY$year/Gift Certificate Log $year.xlsx"
    orders.foreach {
      order => order.codes.foreach {
        code => dropbox.upload(
          logPath,
          excel.appendToLog(dropbox.download(logPath))(
            Calendar.getInstance().getTime,
            order.toName,
            order.from,
            "No Preference",
            s"${order.asSquare.getName} ${
              wrapModifiers(order.asSquare.getModifiers.asScala.map(_.getName).mkString(", "))
            }",
            order.asSquare.getVariationName,
            order.asSquare.getTotalMoney.decimal,
            order.tip.getOrElse(0L) / 100.0,
            "CC",
            code,
          )
        )
      }
    }
  }

}

case class PublicOrder(itemId: String, variationId: String, quantity: Int, from: String, toName: String,
                       toEmail: String, giftMessage: Option[String], modifiers: List[String], tip: Option[Long]) {
  require(!modifiers.contains("RZRULCMMVYYTRDJEIOJVZVEN") || quantity % 2 == 0,
    "You cannot purchase an odd number of massages with the Couples addon")
  val codes: List[String] = Range(0, quantity).toList.map {
    _ => new RandomStringGenerator.Builder().withinRange('A', 'Z').build().generate(8)
  }
  def asSquare(implicit order: Order): OrderLineItem =
    order.getLineItems.asScala.find(_.getCatalogObjectId == variationId).get
}

case class PublicOrderRequest(nonce: String, orders: List[PublicOrder], email: String) {
  require(email.split("@")(1).contains("."),
    s"Bad email address given, make sure to include .com or other top level domain: $email")
}
