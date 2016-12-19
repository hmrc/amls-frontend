package connectors

import config.WSHttp
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.Future

trait BusinessMatchingConnector extends ServicesConfig {

  case class Address(line_1: String,
                     line_2: String,
                     line_3: Option[String],
                     line_4: Option[String],
                     postcode: Option[String] = None,
                     country: String) {
  }

  object Address {
    implicit val formats = Json.format[Address]
  }

  case class Identification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

  case class ReviewDetails(businessName: String,
                           businessType: Option[String],
                           businessAddress: Address,
                           sapNumber: String,
                           safeId: String,
                           isAGroup: Boolean = false,
                           directMatch: Boolean = false,
                           agentReferenceNumber: Option[String],
                           firstName: Option[String] = None,
                           lastName: Option[String] = None,
                           utr: Option[String] = None,
                           identification: Option[Identification] = None,
                           isBusinessDetailsEditable: Boolean = false)

  object Identification {
    implicit val formats = Json.format[Identification]
  }

  object ReviewDetails {
    implicit val formats = Json.format[ReviewDetails]
  }

  val httpGet: HttpGet
  val businessMatchingUrl = s"${baseUrl("business-customer")}/business-customer"
  val serviceName = "amls"

  def getReviewDetails(implicit hc: HeaderCarrier): Future[ReviewDetails] = {

    val url = s"$businessMatchingUrl/fetch-review-details/$serviceName"

    httpGet.GET[ReviewDetails](url)
  }

}

object BusinessMatchingConnector extends BusinessMatchingConnector {
  override val httpGet = WSHttp
}
