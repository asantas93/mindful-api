import controllers.{Order, OrderRequest}
import org.scalatest.{FlatSpec, Matchers}
import services.{PublicItem, SquareCatalog, SquareOrders, SquareTransactions}

class SquareTest extends FlatSpec with Matchers {

  val ordersApi = new SquareOrders
  val transactionsApi = new SquareTransactions
  val catalogApi = new SquareCatalog

  it should "calculate expected order total" in {
    implicit val inventory: List[PublicItem] = catalogApi.getInventory
    val item1 = inventory.head
    val item2 = inventory.last
    val variation1 = item1.variations.last
    val variation2 = item2.variations.head
    val quantity1 = 2
    val quantity2 = 1
    val tip1 = 5
    val modifier1 = item1.modifiers.head.modifiers.head
    val orderRequest = OrderRequest(
      "fake-card-nonce-ok",
      List(
        Order(item1.id, variation1.id, quantity1, "Buyer", "Recipient1", "recipient1@example.com", None, List(modifier1.id), Some(tip1)),
        Order(item2.id, variation2.id, quantity2, "Buyer", "Recipient2", "recipient2@example.com", None, List(), None)
      ),
      "buyer@example.com"
    )
    val order = ordersApi.createOrder(orderRequest)
    println(order)
    assertResult {
      quantity1 * (modifier1.price + tip1 * 100L + variation1.price) + quantity2 * variation2.price
    } {
      order.getTotalMoney.getAmount
    }
  }

}
