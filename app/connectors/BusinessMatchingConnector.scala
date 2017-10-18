/*
 * Copyright 2017 HM Revenue & Customs
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

import config.WSHttp
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HttpGet, NotFoundException }

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

trait BusinessMatchingConnector extends ServicesConfig with HeaderCarrierForPartialsConverter {
  val httpGet: HttpGet
  val businessMatchingUrl = s"${baseUrl("business-customer")}/business-customer"
  val serviceName = "amls"

  def getReviewDetails(implicit request: Request[_]): Future[Option[BusinessMatchingReviewDetails]] = {

    val url = s"$businessMatchingUrl/fetch-review-details/$serviceName"
    val logPrefix = "[BusinessMatchingConnector][getReviewDetails]"

    Logger.debug(s"$logPrefix Fetching $url..")

    httpGet.GET[BusinessMatchingReviewDetails](url) map { result =>
      Logger.debug(s"$logPrefix Finished getting review details. Name: ${result.businessName}")
      Some(result)
    } recoverWith {
      case _: NotFoundException => Future.successful(None)
      case ex =>
        Logger.warn(s"$logPrefix Failed to fetch review details", ex)
        Future.failed(ex)
    }
  }
}

object BusinessMatchingConnector extends BusinessMatchingConnector {
  override val httpGet = WSHttp
  override val crypto = SessionCookieCryptoFilter.encrypt _
}
