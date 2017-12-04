package controllers

import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import org.apache.commons.text.RandomStringGenerator
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.libs.json.{Json, Reads}
import services._

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@Inject
class OrderController @Inject()(cc: ControllerComponents, square: Square, email: Email, sheets: GoogleSheets)
  extends AbstractController(cc) {

  def order = Action {
    request =>
      implicit val inventory: List[PublicItem] = square.inventory
      implicit val orderReads: Reads[Order] = Json.reads[Order]
      implicit val orderRequestReads: Reads[OrderRequest] = Json.reads[OrderRequest]
      Try {
        request.body.asJson.orElse {
          throw new RuntimeException("Your order could not be understood. Please contact our staff for assistance.")
        } map { _.as[OrderRequest] } foreach {
          request =>
            square.charge(request.orders.map { _.squareTotal }.sum, request.nonce)
            tryEmailError { () => logGiftCards(request.orders) }
            tryEmailError { () => email.receiptEmail(request.email, request.orders) }
            tryEmailError { () => request.orders foreach { email.giftEmail } }
        }
      } match {
        case Success(_) => Ok { "Order processed." }
        case Failure(e) => BadRequest {
          s"Your order resulted in an error and has been cancelled:\n${e.getMessage}"
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

  def logGiftCards(orders: List[Order])(implicit inventory: List[PublicItem]): Unit = {
    implicit val credential: GoogleCredential = sheets.credential
    orders.foreach {
      order => order.codes.foreach {
        code => sheets.append(
          code,
          new SimpleDateFormat("MM/dd/yyyy").format(Calendar.getInstance().getTime),
          order.toName,
          order.from,
          "Unspecified",
          order.itemName,
          order.variationName,
          order.unitPrice,
          0,
          "CC",
          "",
          "",
          "",
          "Web purchase"
        )
      }
    }
  }

}

case class Order(private val itemId: String, private val variationId: String, quantity: Int, from: String,
                 toName: String, toEmail: String, giftMessage: Option[String], private val couples: Boolean
                )(implicit inventory: List[PublicItem]) {

  validate()

  val codes: List[String] = Range(0, quantity).toList.map {
    _ => new RandomStringGenerator.Builder().withinRange('A', 'Z').build().generate(8)
  }
  private val item: PublicItem = inventory.find { _.id == itemId }.get
  private val variation: PublicVariation = item.variations.find { _.id == variationId }.get
  private val squarePrice: Int = variation.price + { if (couples) 1000 + variation.price else 0 }
  val squareTotal: Int = squarePrice * quantity
  val unitPrice: Double = squarePrice / 100.0
  val totalPrice: Double = squareTotal / 100.0
  val itemName: String = item.name + (if (couples) " (couples)" else "")
  val variationName: String = variation.name

  def validate(): Unit = {
    require(toEmail.split("@")(1).contains("."),
      s"Bad email address given, make sure to include .com or other top level domain: $toEmail")
    val item = inventory.find { _.id == itemId }
    require(item.isDefined, s"Bad itemId specified for order $this")
    require(item.get.variations.exists { _.id == variationId }, s"Bad variationId specified for order $this")
    require(quantity >= 0, s"Illegal quantity $quantity specified for order $this")
  }

}

case class OrderRequest(nonce: String, orders: List[Order], email: String) {
  require(email.split("@")(1).contains("."),
    s"Bad email address given, make sure to include .com or other top level domain: $email")
}
