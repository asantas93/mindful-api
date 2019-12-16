package biz.mindfulmassage.services

import com.squareup.connect.api.CatalogApi
import com.squareup.connect.models.SearchCatalogObjectsRequest

import scala.collection.JavaConverters._
import scala.util.Try

class SquareCatalog extends SquareService {

  private val api = new CatalogApi(client)
  private lazy val sellableCategories = getCategories filterKeys { List("Massage").contains }

  def getInventory: List[PublicItem] = {
    val modifierLists = getModifierLists
    api.searchCatalogObjects(
      new SearchCatalogObjectsRequest()
        .addObjectTypesItem(SearchCatalogObjectsRequest.ObjectTypesEnum.ITEM)
    ).getObjects.asScala.toList.filter {
      obj => sellableCategories.map(_.swap).contains(obj.getItemData.getCategoryId)
    }.map {
      obj =>
        val itemData = obj.getItemData
        PublicItem(
          obj.getId,
          itemData.getName,
          itemData.getVariations.asScala.toList.flatMap {
            variation => Try {
              val variationData = variation.getItemVariationData
              PublicVariation(variation.getId, variationData.getName, variationData.getPriceMoney.getAmount.intValue())
            }.toOption
          },
          Option(itemData.getDescription),
          itemData.getCategoryId,
          itemData.getModifierListInfo.asScala.toList.map {
             mod => modifierLists(mod.getModifierListId)
          }
        )
    }
  }

  def getCategories: Map[String, String] = api.searchCatalogObjects(
    new SearchCatalogObjectsRequest()
      .addObjectTypesItem(SearchCatalogObjectsRequest.ObjectTypesEnum.CATEGORY)
  ).getObjects.asScala.toList.map {
    obj => obj.getCategoryData.getName -> obj.getId
  }.toMap

  def getModifierLists: Map[String, PublicModifierList] = api.searchCatalogObjects(
    new SearchCatalogObjectsRequest()
      .addObjectTypesItem(SearchCatalogObjectsRequest.ObjectTypesEnum.MODIFIER_LIST)
  ).getObjects.asScala.toList.map {
    obj =>
      val modifierListData = obj.getModifierListData
      obj.getId -> PublicModifierList(
        obj.getId,
        modifierListData.getName,
        modifierListData.getModifiers.asScala.toList.map {
          modifier =>
            val modifierData = modifier.getModifierData
            PublicModifier(modifier.getId, modifierData.getName, modifierData.getPriceMoney.getAmount.intValue())
        }
      )
  }.toMap


}
