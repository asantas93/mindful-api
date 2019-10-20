package biz.mindfulmassage

import java.text.NumberFormat
import java.util.Locale

import com.squareup.connect.models.Money
import org.apache.poi.ss.usermodel.{Cell, CellStyle, Row, Sheet}
import org.json4s._
import play.api.libs.json._

package object implicits {

  implicit class SheetLike(val sheet: Sheet) extends AnyVal {
    def appendRow(): Row = sheet.createRow(sheet.getLastRowNum + 1)
  }

  implicit class RowLike(val row: Row) extends AnyVal {
    def createCellWithStyle(cellNum: Int, cellStyle: CellStyle): Cell = {
      val cell = row.createCell(cellNum)
      cell.setCellStyle(cellStyle)
      cell
    }
  }

  implicit class MoneyLike(val money: Money) extends AnyVal {
    def pretty: String = NumberFormat.getCurrencyInstance(Locale.US).format(decimal)
    def decimal: Double = money.getAmount / 100.0
  }

  implicit class WrappedJsValue(val json: JsValue) extends AnyVal {
    def asJValue: JValue = json match {
      case JsString(str) => JString(str)
      case JsNull => JNull
      case JsBoolean(value) => JBool(value)
      case JsNumber(value) => JDecimal(value)
      case JsArray(items) => JArray(items.map(_.asJValue).toList)
      case JsObject(items) => JObject(items.map { case (k, v) => k -> v.asJValue }.toList)
    }
  }

  implicit class WrappedJValue(val json: JValue) extends AnyVal {
    def asJsValue: JsValue = json match {
      case JString(str) => JsString(str)
      case JNothing => JsNull
      case JNull => JsNull
      case JDecimal(value) => JsNumber(value)
      case JDouble(value) => JsNumber(value)
      case JInt(value) => JsNumber(BigDecimal(value))
      case JLong(value) => JsNumber(BigDecimal(value))
      case JBool(value) => JsBoolean(value)
      case JArray(fields) => JsArray(fields.map(_.asJsValue))
      case JSet(fields) => JsArray(fields.map(_.asJsValue).toList)
      case JObject(fields) => JsObject(fields.map { case (k, v) => k -> v.asJsValue}.toMap)
    }
  }
}
