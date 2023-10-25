/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import config.ApplicationConfig

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson

case class BusinessMatchingAddress(line_1: String,
                                   line_2: Option[String],
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

class BusinessMatchingConnector @Inject()(val http: HttpClient,
                                          val applicationConfig: ApplicationConfig) extends Logging {

  val serviceName = "amls"

  def getReviewDetails(implicit request: Request[_], headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessMatchingReviewDetails]] = {

    val url = s"${applicationConfig.businessMatchingUrl}/fetch-review-details/$serviceName"
    val logPrefix = "[BusinessMatchingConnector][getReviewDetails]"

    // $COVERAGE-OFF$

    logger.debug(s"$logPrefix Fetching $url..")
    // $COVERAGE-ON$

    http.GET[BusinessMatchingReviewDetails](url) map { result =>


      // $COVERAGE-OFF$
      logger.debug(s"$logPrefix Finished getting review details. Name: ${result.businessName}")
      // $COVERAGE-ON$
      Some(result)
    } recoverWith {
      case e : UpstreamErrorResponse if e.statusCode == 404 => Future.successful(None)
      case ex =>
        // $COVERAGE-OFF$
        logger.warn(s"$logPrefix Failed to fetch review details", ex)
        // $COVERAGE-ON$
        Future.failed(ex)
    }
  }
}
