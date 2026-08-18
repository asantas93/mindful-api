package biz.mindfulmassage.lambdas

import java.util.Calendar
import biz.mindfulmassage.InvalidUserInput
import biz.mindfulmassage.implicits._
import biz.mindfulmassage.model.HttpResponse
import biz.mindfulmassage.services._
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.squareup.square.types.{Order, OrderLineItem}
import org.apache.commons.logging.LogFactory
import org.apache.commons.text.RandomStringGenerator
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.{JsonMethods, Serialization}

import java.io.{InputStream, OutputStream, OutputStreamWriter}
import scala.collection.JavaConverters._

class OrderLambda extends RequestStreamHandler {

  private val ordersApi = new SquareOrders
  private val transactionsApi = new SquareTransactions
  private val email = new Email
  private val dropbox = new Dropbox
  private val excel = new Excel
  private val maintainerEmail = biz.mindfulmassage.conf.getString("email.maintainer")
  private val logger = LogFactory.getLog(this.getClass.getName)
  private implicit val formats: Formats = DefaultFormats

  override def handleRequest(input: InputStream, output: OutputStream, ctx: Context): Unit = {
    val headers = Map("Access-Control-Allow-Origin" -> "*")
    val resp = try {
      val ast = JsonMethods.parse(input)
      val body = (ast \ "body").extract[String]
      val orderRequest = JsonMethods.parse(body).extract[PublicOrderRequest]
      logger.info(s"Received order from ${orderRequest.email} with items ${orderRequest.orders}")
      (orderRequest.email :: orderRequest.orders.map(_.toEmail)).foreach(Email.validateEmail)
      implicit val order: Order = ordersApi.createOrder(orderRequest)
      transactionsApi.completeOrder(order, orderRequest)
      tryEmailError {
        logGiftCards(orderRequest.orders)
      }
      tryEmailError {
        email.receiptEmail(orderRequest.email, order)
      }
      tryEmailError {
        orderRequest.orders.foreach { o => email.giftEmail(o) }
      }
      HttpResponse(200, "Order processed.", headers)
    } catch {
      case e: Throwable =>
        logger.error(s"Encountered exception handling request $input", e)
        e match {
          case e: InvalidUserInput => HttpResponse(400, e.getMessage, headers)
          case e: Throwable =>
            email.errorEmail(maintainerEmail, e)
            HttpResponse(500,
              "Your order resulted in an error and has been cancelled. Please try clearing your browser's cache and " +
                "refreshing this page. If the problem persists please contact our staff with this error message:\n" +
                e.getMessage,
              headers
            )
        }
    }
    Serialization.write(resp, new OutputStreamWriter(output))
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
                wrapModifiers(orderLineItem.getModifiers.get.asScala.map(_.getName).mkString(", "))
              }",
              orderLineItem.getVariationName.get,
              orderLineItem.getBasePriceMoney.get().decimal +
                orderLineItem.getModifiers.get.asScala.map(_.getBasePriceMoney.get.decimal).sum,
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
    _ => new RandomStringGenerator.Builder().withinRange('A', 'Z').get().generate(8)
  }
  def asSquare(implicit order: Order): OrderLineItem =
    order.getLineItems.get.asScala.find(_.getCatalogObjectId.get == variationId).get
}

case class PublicOrderRequest(nonce: String, orders: List[PublicOrder], email: String)
