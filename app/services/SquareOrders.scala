package services

import java.util.UUID.randomUUID

import com.squareup.connect.api.OrdersApi
import com.squareup.connect.models._
import controllers.PublicOrderRequest

import scala.collection.JavaConverters._
import scala.language.postfixOps

class SquareOrders extends SquareService {

  private val api = new OrdersApi(client)

  def createOrder(orderRequest: PublicOrderRequest): Order = api.createOrder(
    locationId,
    new CreateOrderRequest()
      .idempotencyKey(randomUUID.toString)
      .lineItems(orderRequest.orders.map {
        order => new CreateOrderRequestLineItem()
          .catalogObjectId(order.variationId)
          .modifiers {
            order.modifiers.map {
              modifierId => new CreateOrderRequestModifier()
                .catalogObjectId(modifierId)
            }.asJava
          }
          .quantity(order.quantity.toString)
      } ::: orderRequest.orders.filter(_.tip.exists(_ > 0)).map {
        order => new CreateOrderRequestLineItem()
          .name("Tip")
          .basePriceMoney(
            new Money()
              .amount((order.tip.get * 100).longValue())
              .currency(Money.CurrencyEnum.USD)
          )
          .quantity(order.quantity.toString)
      } asJava)
  ).getOrder

}

case class PublicVariation(id: String, name: String, price: Int)
case class PublicModifier(id: String, name: String, price: Int)
case class PublicModifierList(id: String, name: String, modifiers: List[PublicModifier])
case class PublicItem(id: String, name: String, variations: List[PublicVariation],
                      description: Option[String], category: String, modifiers: List[PublicModifierList])
