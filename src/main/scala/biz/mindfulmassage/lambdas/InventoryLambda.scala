package biz.mindfulmassage.lambdas

import com.github.dnvriend.lambda.annotation.HttpHandler
import com.github.dnvriend.lambda.{ApiGatewayHandler, HttpRequest, HttpResponse, SamContext}
import org.json4s.Extraction._
import biz.mindfulmassage.implicits._
import biz.mindfulmassage.services.SquareCatalog
import org.json4s.{DefaultFormats, Formats}

@HttpHandler(path = "/inventory", method = "get")
 class InventoryLambda extends ApiGatewayHandler {

  implicit val formats: Formats = DefaultFormats
  private val catalog = new SquareCatalog

  override def handle(request: HttpRequest, ctx: SamContext): HttpResponse = {
    HttpResponse.ok.withBody(decompose(catalog.getInventory).asJsValue)
  }
}