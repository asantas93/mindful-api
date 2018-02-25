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


}
