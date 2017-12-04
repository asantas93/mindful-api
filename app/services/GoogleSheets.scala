package services

import java.io.FileInputStream
import java.util.Collections

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.api.services.sheets.v4.{SheetsScopes, Sheets}

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.language.postfixOps

class GoogleSheets {

  val httpTransport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
  val jsonFactory: JacksonFactory = JacksonFactory.getDefaultInstance

  def credential: GoogleCredential =
    GoogleCredential
      .fromStream(new FileInputStream(".service_key.json"))
      .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))

  def append(data: Any*)(implicit credential: GoogleCredential = credential): Unit = {
    new Sheets.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName("MM Sheets Service")
      .build()
      .spreadsheets()
      .values()
      .append("1iONIaTTxmNz3HfXHIDtzxsokiOHmka-ov35zRW27QaE", "A1:Z10000", new ValueRange()
        .setValues(List(data.toList.asInstanceOf[List[AnyRef]]) map { _.asJava } asJava))
      .setValueInputOption("RAW")
      .execute()
  }

}

