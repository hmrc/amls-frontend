package connectors

import config.WSHttp
import models.AreYouEmployedWithinTheBusinessModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpResponse}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait AreYouEmployedWithinTheBusinessConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("amls")
  lazy val aboutYou = "about-you-2"

  def http: HttpGet with HttpPost

  def submitDetails(areYouEmployedWithinTheBusinessModel: AreYouEmployedWithinTheBusinessModel)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "amls"
    val postUrl = s"""$serviceURL/$baseURI/$aboutYou"""
    val jsonData = Json.toJson(areYouEmployedWithinTheBusinessModel)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }
}

object AreYouEmployedWithinTheBusinessConnector extends AreYouEmployedWithinTheBusinessConnector {
  override def http = WSHttp
}
