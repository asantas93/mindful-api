package biz.mindfulmassage.lambdas

import java.text.SimpleDateFormat
import java.util.Calendar

import biz.mindfulmassage.InvalidUserInput
import biz.mindfulmassage.implicits._
import biz.mindfulmassage.services._
import com.github.dnvriend.lambda.annotation.HttpHandler
import com.github.dnvriend.lambda.{ApiGatewayHandler, HttpRequest, HttpResponse, SamContext}
import com.github.dnvriend.lambda.HttpResponse._
import com.squareup.connect.models.{Order, OrderLineItem}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.text.RandomStringGenerator
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, MappingException}
import play.api.libs.json.JsString

import scala.collection.JavaConverters._

@HttpHandler(path = "/order", method = "options")
class OrderOptionsLambda extends ApiGatewayHandler {

  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    ok
      .withHeader("Access-Control-Allow-Origin", "*")
      .withHeader("Access-Control-Allow-Headers", "*")
      .withHeader("Access-Control-Allow-Methods", "OPTIONS,POST")
  }
}

@HttpHandler(path = "/order", method = "post")
class OrderLambda extends ApiGatewayHandler with LazyLogging {

  implicit val formats: Formats = DefaultFormats
  private val ordersApi = new SquareOrders
  private val transactionsApi = new SquareTransactions
  private val email = new Email
  private val dropbox = new Dropbox
  private val excel = new Excel
  private val maintainerEmail = biz.mindfulmassage.conf.getString("email.maintainer")

  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    val resp = try {
      val orderRequest = request.body.asJValue.extract[PublicOrderRequest]
      logger.info(s"Received order from ${orderRequest.email} with items ${orderRequest.orders}")
      (orderRequest.email :: orderRequest.orders.map(_.toEmail)).foreach(Email.validateEmail)
      implicit val order: Order = ordersApi.createOrder(orderRequest)
      transactionsApi.completeOrder(order, orderRequest)
      tryEmailError { logGiftCards(orderRequest.orders) }
      tryEmailError { email.receiptEmail(orderRequest.email, order) }
      tryEmailError { orderRequest.orders.foreach { o => email.giftEmail(o) } }
      ok.withBody(JsString("Order processed."))
    } catch {
      case e: Throwable =>
        logger.error(s"Encountered exception handling request ${Serialization.writePretty(request.body.asJValue)}", e)
        e match {
          case _: MappingException => validationError.withBody {
            JsString("Your order was sent in an invalid format. Please contact our staff for assistance.")
          }
          case e: InvalidUserInput => validationError.withBody(JsString(e.getMessage))
          case e: Throwable =>
            email.errorEmail(maintainerEmail, e)
            serverError.withBody {
            JsString {
              "Your order resulted in an error and has been cancelled. Please try clearing your browser's cache and " +
                "refreshing this page. If the problem persists please contact our staff with this error message:\n" +
                e.getMessage
            }
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
        email.errorEmail(maintainerEmail, e)
    }
  }

  def logGiftCards(orders: List[PublicOrder])(implicit squareOrder: Order): Unit = {
    def wrapModifiers(m: String) = if (m.nonEmpty) s"with ($m)" else ""
    val logPath = "/MM/Financial Records/Gift Card Logs/Gift Certificate Log - Online.xlsx"
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

case class PublicOrderRequest(nonce: String, orders: List[PublicOrder], email: String)
