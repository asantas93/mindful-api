package biz.mindfulmassage.lambdas

import biz.mindfulmassage.model.HttpResponse
import biz.mindfulmassage.services.{PublicItem, SquareCatalog}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

 class InventoryLambda extends RequestHandler[Void, HttpResponse[List[PublicItem]]] {

  private val catalog = new SquareCatalog

  override def handleRequest(request: Void, ctx: Context): HttpResponse[List[PublicItem]] = {
    HttpResponse(
      200,
      catalog.getInventory,
      Map("Access-Control-Allow-Origin" -> "*")
    )
  }
}