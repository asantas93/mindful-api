package services
import com.squareup.connect.{ApiClient, Configuration}

trait SquareSandboxService extends SquareService {

  private val devMode = sys.env.get("SQUARE_DEV_MODE").isDefined

  override val client: ApiClient = if (devMode) {
    val c = Configuration.getDefaultApiClient
    c.setAccessToken(sys.env("SQUARE_DEV_ACCESS_TOKEN"))
    c
  } else super.client

  override val locationId: String = if (devMode) {
    sys.env("SQUARE_DEV_LOCATION_ID")
  } else super.locationId

}
