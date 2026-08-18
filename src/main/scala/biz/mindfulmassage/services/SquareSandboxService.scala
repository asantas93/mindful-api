package biz.mindfulmassage.services
import com.squareup.square.{SquareClient, SquareClientBuilder}

trait SquareSandboxService extends SquareService {

  private val devMode = sys.env.contains("SQUARE_DEV_MODE")

  override val client: SquareClient = if (devMode) {
    new SquareClientBuilder().token(sys.env("SQUARE_DEV_ACCESS_TOKEN")).build()
  } else super.client

  override val locationId: String = if (devMode) {
    sys.env("SQUARE_DEV_LOCATION_ID")
  } else super.locationId

}
