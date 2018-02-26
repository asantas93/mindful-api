import java.text.NumberFormat
import java.util.Locale

import com.squareup.connect.models.Money
import org.apache.poi.ss.usermodel.{Cell, CellStyle, Row, Sheet}

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

}
