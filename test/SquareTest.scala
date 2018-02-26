import java.io.{File, PrintWriter}

import com.squareup.connect.models.Order
import controllers.{PublicOrder, PublicOrderRequest}
import org.scalatest.{FlatSpec, Matchers}
import services._

class SquareTest extends FlatSpec with Matchers {

  val ordersApi = new SquareOrders
  val transactionsApi = new SquareTransactions
  val catalogApi = new SquareCatalog
  val email = new Email

  it should "calculate expected order total" in {
    implicit val inventory: List[PublicItem] = catalogApi.getInventory
    val item1 = inventory.find(_.name == "Couples Massage").get
    val item2 = inventory.last
    val variation1 = item1.variations.last
    val variation2 = item2.variations.head
    val quantity1 = 2
    val quantity2 = 1
    val tip1 = 500
    val modifier1 = item1.modifiers.head.modifiers.head
    val orderRequest = PublicOrderRequest(
      "fake-card-nonce-ok",
      List(
        PublicOrder(item1.id, variation1.id, quantity1, "Buyer", "Recipient1", "recipient1@example.com", None, List(modifier1.id), Some(tip1)),
        PublicOrder(item2.id, variation2.id, quantity2, "Buyer", "Recipient2", "recipient2@example.com", None, List(), None)
      ),
      "buyer@example.com"
    )
    implicit val order: Order = ordersApi.createOrder(orderRequest)
    assertResult {
      quantity1 * (modifier1.price + tip1 + variation1.price) + quantity2 * variation2.price
    } {
      order.getTotalMoney.getAmount
    }
    orderRequest.orders.foreach {
      o => writeFile(s"${o.from}${o.toName}.html", email.renderGift(o))
    }
    writeFile("receipt.html", email.renderReceipt(order))
  }

  def writeFile(path: String, content: String): Unit = {
    val writer = new PrintWriter(new File(path))
    writer.println(content)
    writer.close()
  }

}
