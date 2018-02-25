package services

import java.util.Calendar

object Demo {

  def main(args: Array[String]): Unit = {
    val dropbox = new Dropbox
    val excel = new Excel
    val currentLog = dropbox.download("/MM/Financial Records/FY2018/Gift Certificate Log 2018 WEB TEST3.xlsx")
    val updatedLog = excel.appendToLog(currentLog)(
      Calendar.getInstance().getTime,
      "toName",
      "fromName",
      "unspecified",
      "massageType",
      "massageLength",
      100,
      10,
      "paymentType",
      "MY ID"
    )
    dropbox.upload("/MM/Financial Records/FY2018/Gift Certificate Log 2018 WEB TEST3.xlsx", updatedLog)
  }

}
