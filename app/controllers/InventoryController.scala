package controllers

import javax.inject.Inject

import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.libs.json.{Json, OWrites}
import services.{PublicItem, PublicVariation, Square}

@Inject
class InventoryController @Inject() (cc: ControllerComponents, square: Square) extends AbstractController(cc) {

  private implicit val variationWrites: OWrites[PublicVariation] = Json.writes[PublicVariation]
  private implicit val itemWrites: OWrites[PublicItem] = Json.writes[PublicItem]

  def inventory = Action {
    Ok { Json.toJson { square.inventory } }
  }

}

