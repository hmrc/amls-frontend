package connectors

import config.WSHttp
import models.LoginDetails
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig

import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AmlsConnector extends ServicesConfig {

  lazy val serviceURL = baseUrl("amls")

  val login = "login"

  val http: HttpGet with HttpPost = WSHttp

  def responseTo[T](uri: String)(response: HttpResponse)(implicit rds: Reads[T]) = response.json.as[T]

  def submitLoginDetails(loginDetails: LoginDetails)(implicit headerCarrier: HeaderCarrier) :Future[Option[LoginDetails]] = {
    val baseURI = "amls"
    val postUrl = s"""localhost:8490/amls/login"""
    val jsonData = Json.toJson(loginDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData).map(responseTo[Option[LoginDetails]](postUrl))
  }

}

object AmlsConnector extends AmlsConnector
