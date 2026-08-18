package biz.mindfulmassage.services

import com.squareup.square.{SquareClient, SquareClientBuilder}


trait SquareService {

  private val location = biz.mindfulmassage.conf.getString("square.location")
  private val token = biz.mindfulmassage.conf.getString("square.access-token")

  private val clientVal = new SquareClientBuilder().token(token).build()

  def locationId: String = location
  def client: SquareClient = this.clientVal

}
