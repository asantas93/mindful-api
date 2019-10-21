package biz.mindfulmassage.services

import java.io._
import java.util.Date

import org.apache.poi.ss.usermodel.{HorizontalAlignment, WorkbookFactory}
import biz.mindfulmassage.implicits.{RowLike, SheetLike}

class Excel {

  def appendToLog(inputStream: InputStream)(
                   date: Date,
                   toName: String,
                   fromName: String,
                   therapistRequested: String,
                   massageType: String,
                   massageLength: String,
                   costOfService: Double,
                   tip: Double,
                   paymentType: String,
                   notes: String
  ): InputStream = {
    val workbook = WorkbookFactory.create(inputStream)
    val helper = workbook.getCreationHelper
    val dateStyle = {
      val style = workbook.createCellStyle()
      style.setDataFormat(helper.createDataFormat().getFormat("mm/dd/yyyy"))
      style.setAlignment(HorizontalAlignment.CENTER)
      style
    }
    val stringStyle = {
      val style = workbook.createCellStyle()
      style.setAlignment(HorizontalAlignment.CENTER)
      style
    }
    val dollarStyle = {
      val style = workbook.createCellStyle()
      style.setDataFormat(helper.createDataFormat().getFormat("$0.00"))
      style
    }
    val row = workbook.getSheetAt(0).appendRow()
    row.createCellWithStyle(0, dateStyle).setCellValue(date)
    row.createCellWithStyle(1, stringStyle).setCellValue(toName)
    row.createCellWithStyle(2, stringStyle).setCellValue(fromName)
    row.createCellWithStyle(3, stringStyle).setCellValue(therapistRequested)
    row.createCellWithStyle(4, stringStyle).setCellValue(massageType)
    row.createCellWithStyle(5, stringStyle).setCellValue(massageLength)
    row.createCellWithStyle(6, dollarStyle).setCellValue(costOfService)
    row.createCellWithStyle(7, dollarStyle).setCellValue(tip)
    row.createCellWithStyle(8, stringStyle).setCellValue(paymentType)
    // 9, 10, 11 not valid for web
    row.createCellWithStyle(12, stringStyle).setCellValue(notes)
    val outputStream = new ByteArrayOutputStream()
    workbook.write(outputStream)
    val data = outputStream.toByteArray
    outputStream.close()
    workbook.close()
    new ByteArrayInputStream(data)
  }


}
