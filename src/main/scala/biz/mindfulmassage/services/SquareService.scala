package biz.mindfulmassage.services

import com.squareup.connect.{ApiClient, Configuration}

trait SquareService {

  private val location = biz.mindfulmassage.conf.getString("square.location")
  private val token = biz.mindfulmassage.conf.getString("square.access-token")

  private val clientVal = {
    val c = Configuration.getDefaultApiClient
    c.setAccessToken(token)
    c
  }

  def locationId: String = location
  def client: ApiClient = this.clientVal

}
