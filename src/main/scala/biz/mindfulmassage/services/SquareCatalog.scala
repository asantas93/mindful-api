package biz.mindfulmassage.services

import com.squareup.square.types.{CatalogObjectType, SearchCatalogItemsRequest, SearchCatalogObjectsRequest}

import scala.collection.JavaConverters._
import scala.util.Try

class SquareCatalog extends SquareService {

  private lazy val sellableCategories = getCategories filterKeys { List("Massage").contains }

  def getInventory: List[PublicItem] = {
    val modifierLists = getModifierLists
    client.catalog().searchItems(
      SearchCatalogItemsRequest.builder()
        .categoryIds(sellableCategories.values.toList.asJava)
        .build()
    ).getItems.get.asScala.map {
      obj =>
        val itemData = obj.getItem.get.getItemData.get
        PublicItem(
          obj.getItem.get.getId,
          itemData.getName.get,
          itemData.getVariations.get.asScala.toList.flatMap {
            variation => Try {
              val variationData = variation.getItemVariation.get.getItemVariationData.get
              PublicVariation(variation.getItemVariation.get().getId, variationData.getName.get, variationData.getPriceMoney.get.getAmount.get().intValue())
            }.toOption
          },
          Option(itemData.getDescription.get),
          itemData.getCategoryId.get,
          itemData.getModifierListInfo.get.asScala.toList.map {
             mod => modifierLists(mod.getModifierListId)
          }
        )
    }.toList
  }

  def getCategories: Map[String, String] = client.catalog().search(
    SearchCatalogObjectsRequest.builder()
      .objectTypes(List(CatalogObjectType.CATEGORY).asJava)
      .build()
  ).getObjects.get.asScala.toList.map {
    obj => {
      val category = obj.getCategory.get
        category.getCategoryData.get().getName.get() -> category.getId.get
    }
  }.toMap

  def getModifierLists: Map[String, PublicModifierList] = client.catalog().search(
    SearchCatalogObjectsRequest.builder()
      .objectTypes(List(CatalogObjectType.MODIFIER_LIST).asJava)
      .build()
  ).getObjects.get.asScala.toList.map {
    obj =>
      val modifier = obj.getModifierList.get
      val modifierListData = modifier.getModifierListData.get
      obj.getModifierList.get().getId -> PublicModifierList(
        modifier.getId,
        modifierListData.getName.get,
        modifierListData.getModifiers.get.asScala.toList.map {
          m =>
            val modifierData = m.getModifier.get().getModifierData.get
            PublicModifier(m.getModifier.get.getId, modifierData.getName.get, modifierData.getPriceMoney.get.getAmount.get.intValue())
        }
      )
  }.toMap


}
