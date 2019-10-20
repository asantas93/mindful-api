package biz.mindfulmassage.services

import com.squareup.connect.{ApiClient, Configuration}

trait SquareService {

  private val clientVal = {
    val c = Configuration.getDefaultApiClient
    c.setAccessToken(sys.env("SQUARE_ACCESS_TOKEN"))
    c
  }
  def locationId: String = sys.env("SQUARE_LOCATION_ID")
  def client: ApiClient = this.clientVal

}
