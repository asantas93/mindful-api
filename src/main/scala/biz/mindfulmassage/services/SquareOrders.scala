package biz.mindfulmassage.services

import java.util.UUID.randomUUID
import biz.mindfulmassage.InvalidUserInput
import biz.mindfulmassage.lambdas.PublicOrderRequest
import com.squareup.square.types.{CreateOrderRequest, Currency, Money, Order, OrderLineItem, OrderLineItemModifier}

import scala.collection.JavaConverters._
import scala.language.postfixOps

class SquareOrders extends SquareService {

  def createOrder(orderRequest: PublicOrderRequest): Order = {
    orderRequest.orders.flatMap(_.tip).find(_ < 0).foreach { badTip =>
      throw InvalidUserInput(s"You cannot leave a negative tip: '$badTip'")
    }
    val resp = client.orders().create(
      CreateOrderRequest.builder()
        .idempotencyKey(randomUUID.toString)
        .order(
          Order.builder()
            .locationId(locationId)
            .lineItems((orderRequest.orders.map {
              order => OrderLineItem.builder()
                .quantity(order.quantity.toString)
                .catalogObjectId(order.variationId)
                .modifiers {
                  order.modifiers.map {
                    modifierId => OrderLineItemModifier.builder()
                      .catalogObjectId(modifierId)
                      .build()
                  }.asJava
                }
                .build()
            } ::: orderRequest.orders.flatMap(o => o.tip.filter(_ > 0).map(o.quantity -> _)).map {
              case (quantity, tip) => OrderLineItem.builder()
                .quantity(quantity.toString)
                .name("Tip")
                .basePriceMoney(
                  Money.builder()
                    .amount((tip * 100).longValue())
                    .currency(Currency.USD)
                    .build()
                )
                .build()
            }).asJava)
            .build()
        ).build()
    )
    if (resp.getErrors.isPresent && !resp.getErrors.get.isEmpty) {
      throw new RuntimeException("Square returned API errors during order creation: " + resp.getErrors.get.asScala.map(_.toString).mkString(", "))
    } else if (resp.getOrder.isPresent) {
      resp.getOrder.get
    } else {
      throw new RuntimeException("No order returned by Square")
    }
  }
}

case class PublicVariation(id: String, name: String, price: Int)
case class PublicModifier(id: String, name: String, price: Int)
case class PublicModifierList(id: String, name: String, modifiers: List[PublicModifier])
case class PublicItem(id: String, name: String, variations: List[PublicVariation],
                      description: Option[String], category: String, modifiers: List[PublicModifierList])
