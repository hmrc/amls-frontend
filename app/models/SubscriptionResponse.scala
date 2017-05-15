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

package models

import play.api.libs.json.Json


case class SubscriptionResponse(
                                 etmpFormBundleNumber: String,
                                 amlsRefNo: String,
                                 registrationFee: BigDecimal,
                                 fpFee: Option[BigDecimal],
                                 fpFeeRate: Option[BigDecimal],
                                 premiseFee: BigDecimal,
                                 premiseFeeRate: Option[BigDecimal],
                                 totalFees: BigDecimal,
                                 paymentReference: String
                               ) extends SubmissionResponse

object SubscriptionResponse {

  val key = "Subscription"

  implicit val format = Json.format[SubscriptionResponse]
}
