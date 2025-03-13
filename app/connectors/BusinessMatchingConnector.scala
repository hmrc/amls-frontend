/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class BusinessMatchingAddress(
  line_1: String,
  line_2: Option[String],
  line_3: Option[String],
  line_4: Option[String],
  postcode: Option[String] = None,
  country: String
) {}

object BusinessMatchingAddress {
  implicit val formats: OFormat[BusinessMatchingAddress] = Json.format[BusinessMatchingAddress]
}

case class BusinessMatchingIdentification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object BusinessMatchingIdentification {
  implicit val formats: OFormat[BusinessMatchingIdentification] = Json.format[BusinessMatchingIdentification]
}

case class BusinessMatchingReviewDetails(
  businessName: String,
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
  isBusinessDetailsEditable: Boolean = false
)

object BusinessMatchingReviewDetails {
  implicit val formats: OFormat[BusinessMatchingReviewDetails] = Json.format[BusinessMatchingReviewDetails]
}

class BusinessMatchingConnector @Inject() (val http: HttpClientV2, val applicationConfig: ApplicationConfig)
    extends Logging {

  val serviceName = "amls"

  def getReviewDetails(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[BusinessMatchingReviewDetails]] = {
    val url = url"${applicationConfig.businessMatchingUrl}/fetch-review-details/$serviceName"
    http.get(url).execute[Option[BusinessMatchingReviewDetails]]
  }
}
