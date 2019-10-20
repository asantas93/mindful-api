package biz.mindfulmassage.lambdas

import java.text.SimpleDateFormat
import java.util.Calendar

import biz.mindfulmassage.implicits._
import biz.mindfulmassage.services._
import com.github.dnvriend.lambda.annotation.HttpHandler
import com.github.dnvriend.lambda.{ApiGatewayHandler, HttpRequest, HttpResponse, SamContext}
import com.squareup.connect.models.{Order, OrderLineItem}
import org.apache.commons.text.RandomStringGenerator
import org.json4s.Extraction._
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsString

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@HttpHandler(path = "/order", method = "options")
class OrderOptionsLambda extends ApiGatewayHandler {

  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    HttpResponse.ok
      .withHeader("Access-Control-Allow-Origin", "*")
      .withHeader("Access-Control-Allow-Headers", "*")
      .withHeader("Access-Control-Allow-Methods", "OPTIONS,POST")
  }
}

@HttpHandler(path = "/order", method = "post")
class OrderLambda extends ApiGatewayHandler {

  implicit val formats: Formats = DefaultFormats
  private val ordersApi = new SquareOrders
  private val transactionsApi = new SquareTransactions
  private val email = new Email
  private val dropbox = new Dropbox
  private val excel = new Excel
  private val maintainerEmail = biz.mindfulmassage.conf.getString("email.maintainer-address")
  private val logger = LoggerFactory.getLogger(getClass)

  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    val resp = Try {
      extractOpt[PublicOrderRequest](request.body.asJValue).orElse {
        throw new RuntimeException("Your order could not be understood. Please contact our staff for assistance.")
      }.foreach {
        request =>
          implicit val order: Order = ordersApi.createOrder(request)
          transactionsApi.completeOrder(order, request)
          tryEmailError { logGiftCards(request.orders) }
          tryEmailError { email.receiptEmail(request.email, order) }
          tryEmailError { request.orders foreach { o => email.giftEmail(o) } }
      }
    } match {
      case Success(_) => HttpResponse.ok
        .withBody(JsString("Order processed."))
      case Failure(e) => HttpResponse.serverError.withBody {
        JsString {
          "Your order resulted in an error and has been cancelled. Please try clearing your browser's cache and " +
            "refreshing this page. If the problem persists please contact our staff with this error message:\n" +
            e.getMessage
        }
      }
    }
    resp.withHeader("Access-Control-Allow-Origin", "*")
  }

  private def tryEmailError(func: => Unit): Unit = {
    try {
      func
    } catch {
      case e: Exception =>
        logger.error("Encountered error in order completion.", e)
        email.genericEmail(
          maintainerEmail,
          s"API Error ${Calendar.getInstance().getTime.toString}",
          s"Encountered error: ${e.toString}\n${e.getMessage}\n${e.getStackTrace.mkString("\n")}",
        )
    }
  }

  def logGiftCards(orders: List[PublicOrder])(implicit squareOrder: Order): Unit = {
    def wrapModifiers(m: String) = if (m.nonEmpty) s"with ($m)" else ""
    val year = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime)
    val logPath = s"/MM/Financial Records/FY$year/Gift Certificate Log - Online $year.xlsx"
    orders.foreach {
      order =>
        val orderLineItem = order.asSquare
        order.codes.foreach {
          code => dropbox.upload(
            logPath,
            excel.appendToLog(dropbox.download(logPath))(
              Calendar.getInstance().getTime,
              order.toName,
              order.from,
              "No Preference",
              s"${orderLineItem.getName} ${
                wrapModifiers(orderLineItem.getModifiers.asScala.map(_.getName).mkString(", "))
              }",
              orderLineItem.getVariationName,
              orderLineItem.getBasePriceMoney.decimal +
                orderLineItem.getModifiers.asScala.map(_.getBasePriceMoney.decimal).sum,
              order.tip.getOrElse(0),
              "CC",
              code,
            )
          )
        }
    }
  }

}

case class PublicOrder(itemId: String, variationId: String, quantity: Int, from: String, toName: String,
                       toEmail: String, giftMessage: Option[String], modifiers: List[String], tip: Option[Double]) {
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
