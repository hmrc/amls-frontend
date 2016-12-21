package connectors

import config.WSHttp
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


case class BusinessMatchingAddress(line_1: String,
                                   line_2: String,
                                   line_3: Option[String],
                                   line_4: Option[String],
                                   postcode: Option[String] = None,
                                   country: String) {
}

object BusinessMatchingAddress {
  implicit val formats = Json.format[BusinessMatchingAddress]
}

case class BusinessMatchingIdentification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object BusinessMatchingIdentification {
  implicit val formats = Json.format[BusinessMatchingIdentification]
}

case class BusinessMatchingReviewDetails(businessName: String,
                                         businessType: Option[String],
                                         businessAddress: BusinessMatchingAddress,
                                         sapNumber: String,
                                         safeId: String,
                                         isAGroup: Boolean = false,
                                         directMatch: Boolean = false,
                                         agentReferenceNumber: Option[String],
                                         firstName: Option[String] = None,
                                         lastName: Option[String] = None,
                                         utr: Option[String] = None,
                                         identification: Option[BusinessMatchingIdentification] = None,
                                         isBusinessDetailsEditable: Boolean = false)

object BusinessMatchingReviewDetails {
  implicit val formats = Json.format[BusinessMatchingReviewDetails]
}

trait BusinessMatchingConnector extends ServicesConfig {

  val httpGet: HttpGet
  val businessMatchingUrl = s"${baseUrl("business-customer")}/business-customer"
  val serviceName = "amls"

  def getReviewDetails(implicit hc: HeaderCarrier): Future[Option[BusinessMatchingReviewDetails]] = {

    val url = s"$businessMatchingUrl/fetch-review-details/$serviceName"
    val logPrefix = "[BusinessMatchingConnector][getReviewDetails]"

    Logger.debug(s"$logPrefix Fetching $url..")

    httpGet.GET[BusinessMatchingReviewDetails](url) map { result =>
      Logger.debug(s"$logPrefix Finished getting review details. Name: ${result.businessName}")
      Some(result)
    } recover {
      case ex =>
        Logger.error(s"$logPrefix Failed to fetch review details: ${ex.getMessage}")
        None
    }
  }

}

object BusinessMatchingConnector extends BusinessMatchingConnector {
  override val httpGet = WSHttp
}
