package connectors

import config.WSHttp
import models.businesscustomer.ReviewDetails
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BusinessMatchingConnector extends ServicesConfig {

  val http = WSHttp
  val businessMatchingUrl = baseUrl("business-matching")
  val serviceName = "amls"

  def getReviewDetails(implicit hc: HeaderCarrier): Future[ReviewDetails] = {
    val url = s"$businessMatchingUrl/fetch-review-details/$serviceName"

    http.GET[ReviewDetails](url)
  }

}

object BusinessMatchingConnector extends BusinessMatchingConnector
