package biz.mindfulmassage.services

import java.util.UUID.randomUUID
import biz.mindfulmassage.lambdas.PublicOrderRequest
import com.squareup.square.types.{CreatePaymentRequest, Order}

class SquareTransactions extends SquareSandboxService {

  def completeOrder(order: Order, orderRequest: PublicOrderRequest): Unit = {
    val idemKey = randomUUID().toString
    client.payments().create(
      CreatePaymentRequest.builder()
        .sourceId(orderRequest.nonce)
        .idempotencyKey(idemKey)
        .locationId(locationId)
        .orderId(order.getId.get())
        .buyerEmailAddress(orderRequest.email)
        .amountMoney(order.getTotalMoney)
        .build()
    )
  }

}
