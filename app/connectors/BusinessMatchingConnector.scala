package connectors

import config.WSHttp
import models.businesscustomer.ReviewDetails
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

trait BusinessMatchingConnector extends ServicesConfig {

  val http = WSHttp
  val baseUrl = baseUrl("business-matching")
  val serviceName = "amls"

  def getReviewDetails: Future[ReviewDetails] = {
    val url = s"$baseUrl/fetch-review-details/$serviceName"

    http.GET[ReviewDetails](url)
  }

}

object BusinessMatchingConnector extends BusinessMatchingConnector
