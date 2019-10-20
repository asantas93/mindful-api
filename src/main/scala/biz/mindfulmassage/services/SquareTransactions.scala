package biz.mindfulmassage.services

import java.util.UUID.randomUUID

import com.squareup.connect.api.TransactionsApi
import com.squareup.connect.models.{ChargeRequest, Order}
import biz.mindfulmassage.lambdas.PublicOrderRequest

class SquareTransactions extends SquareSandboxService {

  private val api = new TransactionsApi(client)

  def completeOrder(order: Order, orderRequest: PublicOrderRequest): Unit = api.charge(
    order.getLocationId,
    new ChargeRequest()
      .orderId(order.getId)
      .buyerEmailAddress(orderRequest.email)
      .cardNonce(orderRequest.nonce)
      .idempotencyKey(randomUUID().toString)
      .amountMoney(order.getTotalMoney)
  )

}
