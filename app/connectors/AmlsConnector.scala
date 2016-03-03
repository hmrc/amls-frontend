package connectors

import config.WSHttp
import models.{SubscriptionRequest, LoginDetails}
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext

import uk.gov.hmrc.play.http._

import scala.concurrent.Future

trait AmlsConnector extends ServicesConfig {
  val serviceURL : String
  val login = "login"
  val http: HttpGet with HttpPost = WSHttp

  def submitLoginDetails(loginDetails: LoginDetails)(implicit user: AuthContext,  headerCarrier: HeaderCarrier) :Future[HttpResponse] = {
    val baseURI = "amls"
    val postUrl = s"""$serviceURL/$baseURI/$login"""
    val jsonData = Json.toJson(loginDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def subscribe(subscriptionRequest:SubscriptionRequest, safeId:String) (implicit user: AuthContext,  headerCarrier: HeaderCarrier) :Future[HttpResponse] = {
    val baseURI = "amls"
    val postUrl = s"""$serviceURL$baseURI/subscription/$safeId"""
    val jsonData = Json.toJson(subscriptionRequest)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }
}

object AmlsConnector extends AmlsConnector {
  override val serviceURL = baseUrl("amls")
}
