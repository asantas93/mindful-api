package biz.mindfulmassage.services

import java.text.SimpleDateFormat
import java.util.Calendar

import javax.mail.internet.InternetAddress
import com.squareup.connect.models.Order
import biz.mindfulmassage.lambdas.PublicOrder
import biz.mindfulmassage.implicits._
import courier.Defaults._
import courier.{Envelope, Mailer, Multipart, addr}

import scala.collection.JavaConverters._
import scala.language.{implicitConversions, postfixOps}

class Email {

  def genericEmail(to: String, subject: String, body: String, bcc: InternetAddress*): Unit = {
    Mailer("smtp.gmail.com", 587)
      .auth(true)
      .as(sys.env("STORE_GMAIL_USER") at "gmail.com" getAddress, sys.env("STORE_GMAIL_PW"))
      .startTtls(true)() {
        Envelope.from(sys.env("STORE_GMAIL_USER") at "gmail.com")
          .to(to)
          .bcc(bcc:_*)
          .subject(subject)
          .content(Multipart().html(body))
      } onComplete { outcome => if (outcome isFailure) throw new RuntimeException("Failed to send email") }
  }

  def renderGift(order: PublicOrder)(implicit squareOrder: Order): String = {
    val orderItem = order.asSquare
    val from = order.from
    val message = order.giftMessage.getOrElse("")
    val quantity = order.quantity
    val variation = order.asSquare.getVariationName
    val item = orderItem.getName
    val modifiers = orderItem.getModifiers.asScala.map(_.getName).mkString(", ")
    val to = order.toName
    val expiration = {
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.YEAR, 1)
      new SimpleDateFormat("MM/dd/yyyy").format(calendar.getTime)
    }
    val codes = order.codes.mkString(", ")
    s"""<!DOCTYPE html>
         |<html lang="en">
         |  <head>
         |    <link href="https://fonts.googleapis.com/css?family=Work+Sans" rel="stylesheet">
         |  </head>
         |  <body>
         |    <h3>$to, you've received a gift from $from${if(order.tip.exists(_ > 0)) " with a tip left on your behalf" else ""}:</h3>
         |    <p style="font-size: 14px; font-style: italic">$message</p>
         |    <br>
         |    <img style="display: block;" src="https://mindfulmassage.biz/img/gcheader.png">
         |    <img style="display: block;" src="https://mindfulmassage.biz/img/gift_card_image.png">
         |    <div style="background-color: #0f0a06; width: 1280px; height: 80px; color: white; font-family: sans;">
         |      <div style="display: inline-block">
         |      <div style="font-size: 25px; padding-left: 15px; padding-top: 15px;">$variation $item</div>
         |      <div style="padding-left: 15px; padding-top: 2px;">expires $expiration</div>
         |      </div>
         |      <div style="float: right; display: inline-block; font-size: 22px; text-align: right; padding-top: 12px; padding-right: 12px;">
         |        <div>2229 E Park Ave Valdosta, GA 31602</div>
         |        <a href="https://mindfulmassage.biz" style="text-decoration: none; color: #ffffff;">mindfulmassage.biz</a>
         |      </div>
         |    </div>
         |    <br>
         |    x$quantity $variation $item ${if (modifiers.nonEmpty) s"with $modifiers" else ""}
         |    <br>
         |    Redeem with code(s) $codes.
         |    <br>
         |    2229 E Park Ave
         |    <br>
         |    Valdosta, GA 31602
         |    <br>
         |    Visit our <a href="https://mindfulmassage.biz">website</a> or call (229) 259-9535 to book your appointment
         |  </body>
         |</html>""".stripMargin
  }

  def giftEmail(order: PublicOrder)(implicit squareOrder: Order): Unit = {
    genericEmail(
      order.toEmail,
      "You've received a gift certificate",
      renderGift(order)
    )
  }

  def renderReceipt(squareOrder: Order): String = {
    val items = squareOrder.getLineItems.asScala.map {
      order => s"${order.getQuantity}x ${Option(order.getVariationName).getOrElse("")} ${order.getName} @ ${
        order.getBasePriceMoney.pretty
      }<br>${order.getModifiers.asScala.map(m => s"- add ${m.getName} @ ${m.getBasePriceMoney.pretty}").mkString("<br>")}"
    }.mkString("<br>")
    val total = squareOrder.getTotalMoney.pretty
     s"""<!DOCTYPE html>
    |<html lang="en">
    |  <body>
    |    <h2>Mindful Massage &amp; Bodywork</h2>
    |    <p>Thank you very much for your order! Below is summary of your purchase:</p>
    |    <br>
    |    $items
    |    <br>
    |    Total: $total
    |    <br>
    |    Please call our office or respond to this email with any questions.
    |  </body>
    |</html>""".stripMargin
  }

  def receiptEmail(email: String, squareOrder: Order): Unit = {
    genericEmail(
      email,
      "Your recent order",
      renderReceipt(squareOrder),
      sys.env("RECEIPT_BCC")
    )
  }

  private implicit def str2IntAddr(email: String): InternetAddress = new InternetAddress(email)

}
