package controllers

import javax.inject.Inject

import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.libs.json.{Json, OWrites}
import services._

@Inject
class InventoryController @Inject() (cc: ControllerComponents, squareCatalog: SquareCatalog) extends AbstractController(cc) {

  private implicit val variationWrites: OWrites[PublicVariation] = Json.writes[PublicVariation]
  private implicit val modifierWrites: OWrites[PublicModifier] = Json.writes[PublicModifier]
  private implicit val modifierListWrites: OWrites[PublicModifierList] = Json.writes[PublicModifierList]
  private implicit val itemWrites: OWrites[PublicItem] = Json.writes[PublicItem]

  def inventory = Action {
    Ok { Json.toJson { squareCatalog.getInventory } }
  }

}

