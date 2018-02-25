package services

import java.util.UUID.randomUUID

import org.json4s.DefaultFormats
import org.json4s.native.parseJson
import play.api.libs.json.{Json, OWrites}

import scala.language.{implicitConversions, postfixOps}
import scalaj.http.Http

class OldSquare {

  private implicit val formats: DefaultFormats = DefaultFormats
  private implicit val priceWrites: OWrites[Price] = Json.writes[Price]
  private implicit val chargeWrites: OWrites[Charge] = Json.writes[Charge]
  private implicit val lineItemWrites: OWrites[LineItem] = Json.writes[LineItem]
  private implicit val orderWrites: OWrites[Order] = Json.writes[Order]

  private def accessToken = sys.env("SQUARE_ACCESS_TOKEN")
  private def devAccessToken = sys.env("SQUARE_DEV_ACCESS_TOKEN")
  private def locationId = sys.env("SQUARE_LOCATION_ID")
  private def devLocationId = sys.env("SQUARE_DEV_LOCATION_ID")
  private def devMode = sys.env.get("SQUARE_DEV_MODE").isDefined
  private val squareUrl = "https://connect.squareup.com"
  private lazy val sellableCategories = categories filterKeys { List("Massage").contains }

  private def squareGet(endpoint: String, params: (String, String)*): String = {
    Http(s"$squareUrl/$endpoint").
      params(params).
      header("Accept", "application/json").
      header("Authorization", s"Bearer $accessToken").
      asString.
      throwError.
      body
  }

  private def squarePost(endpoint: String, data: String): String = {
    Http(s"$squareUrl/$endpoint").
      postData(data).
      header("Accept", "application/json").
      header("Content-Type", "application/json").
      header("Authorization", s"Bearer ${if (devMode) devAccessToken else accessToken}").
      asString.
      throwError.
      body
  }

  def order(): String = {
    // TODO: Come up with way to handle couples massages and switch to order + charge
    squarePost(
      s"v2/locations/${if (devMode) devLocationId else locationId}/orders",
      Json.toJson {
        Order(randomUUID toString, List(LineItem("DNI5KHQIAGVTUCQJBS3YH7F7", "2")))
      } toString
    )
  }

  def charge(amount: Int, nonce: String): Unit = {
    squarePost(
      s"v2/locations/${if (devMode) devLocationId else locationId}/transactions",
      Json.toJson {
        Charge(nonce, Price(amount, "USD"), randomUUID toString)
      } toString
    )
  }

  def inventory: List[PublicItem] = {
    parseJson(squareGet("v2/catalog/list", "types" -> "item")).extract[CatalogItemResponse].
      objects.filter { _.isDefined } map { _.get }
  }

  def categories: Map[String, String] = {
    parseJson(squareGet("v2/catalog/list", "types" -> "category")).extract[CatalogCategoryResponse].objects.
    map { category => category.category_data.name -> category.id } toMap
  }

  implicit def item2publicItem(i: Item): Option[PublicItem] = {
    Option(i) filter {
      _.item_data.category_id.exists { sellableCategories.values.toSeq.contains }
    } map {
      item => PublicItem(
        item.id,
        item.item_data.name,
        item.item_data.variations map {
          variation => PublicVariation(
            variation.id,
            variation.item_variation_data.name,
            variation.item_variation_data.price_money.amount
          )
        },
        item.item_data.description,
        sellableCategories map { _.swap } apply item.item_data.category_id.get,
        Nil
      )
    }
  }

  private case class CatalogItemResponse(objects: List[Item])
  private case class Item(id: String, item_data: ItemData)
  private case class ItemData(name: String, description: Option[String], category_id: Option[String],
                              image_url: Option[String], variations: List[Variation], visibility: String)
  private case class Variation(id: String, item_variation_data: VariationData)
  private case class VariationData(item_id: String, name: String, price_money: Price)
  private case class Price(amount: Int, currency: String)

  private case class CatalogCategoryResponse(objects: List[Category])
  private case class Category(id: String, category_data: CategoryData)
  private case class CategoryData(name: String)

  private case class Charge(card_nonce: String, amount_money: Price, idempotency_key: String)
  private case class Order(idempotency_key: String, line_items: List[LineItem])
  private case class LineItem(catalog_object_id: String, quantity: String)

}
