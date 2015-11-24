package connectors

import config.WSHttp
import models.LoginDetails
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext

import uk.gov.hmrc.play.http._

import scala.concurrent.Future

trait AmlsConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("amls")

  val login = "login"

  val http: HttpGet with HttpPost = WSHttp

  def submitLoginDetails(loginDetails: LoginDetails)(implicit user: AuthContext,  headerCarrier: HeaderCarrier) :Future[HttpResponse] = {
    val baseURI = "amls"
    val postUrl = s"""$serviceURL/$baseURI/$login"""
    val jsonData = Json.toJson(loginDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }
}

object AmlsConnector extends AmlsConnector
