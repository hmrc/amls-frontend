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

package models

case class SubscriptionResponse(
  etmpFormBundleNumber: String,
  amlsRefNo: String,
  subscriptionFees: Option[SubscriptionFees],
  previouslySubmitted: Option[Boolean] = None
) extends SubmissionResponse {

  override def getRegistrationFee: BigDecimal = subscriptionFees.fold(BigDecimal(0)) {
    _.registrationFee
  }

  override def getPremiseFeeRate: Option[BigDecimal] = subscriptionFees.flatMap(fees => fees.premiseFeeRate)

  override def getFpFeeRate: Option[BigDecimal] = subscriptionFees.flatMap(fees => fees.fpFeeRate)

  override def getFpFee: Option[BigDecimal] = subscriptionFees.flatMap(fees => fees.fpFee)

  override def getApprovalCheckFee: Option[BigDecimal] = subscriptionFees.flatMap(fees => fees.approvalCheckFee)

  override def getApprovalCheckFeeRate: Option[BigDecimal] = subscriptionFees.flatMap(fees => fees.approvalCheckFeeRate)

  override def getPremiseFee: BigDecimal = subscriptionFees.fold(BigDecimal(0)) {
    _.premiseFee
  }

  override def getPaymentReference: String = subscriptionFees.fold("") {
    _.paymentReference
  }

  override def getTotalFees: BigDecimal = subscriptionFees.fold(BigDecimal(0)) {
    _.totalFees
  }
}

object SubscriptionResponse {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  val key = "Subscription"

  implicit val format: OFormat[SubscriptionResponse]             = Json.format[SubscriptionResponse]
  implicit val formatOption: Reads[Option[SubscriptionResponse]] = Reads.optionNoError[SubscriptionResponse]

  val oldFeesStructureTransformer: Reads[JsObject] =
    (
      (__ \ Symbol("subscriptionFees") \ Symbol("paymentReference")).json.copyFrom(
        (__ \ Symbol("paymentReference")).json.pick
      ) and
        (__ \ Symbol("subscriptionFees") \ Symbol("registrationFee")).json.copyFrom(
          (__ \ Symbol("registrationFee")).json.pick
        ) and
        (__ \ Symbol("subscriptionFees") \ Symbol("fpFee")).json.copyFrom((__ \ Symbol("fpFee")).json.pick) and
        (__ \ Symbol("subscriptionFees") \ Symbol("fpFeeRate")).json.copyFrom((__ \ Symbol("fpFeeRate")).json.pick) and
        (__ \ Symbol("subscriptionFees") \ Symbol("approvalCheckFee")).json.copyFrom(
          (__ \ Symbol("approvalCheckFee")).json.pick
        ) and
        (__ \ Symbol("subscriptionFees") \ Symbol("approvalCheckFeeRate")).json.copyFrom(
          (__ \ Symbol("approvalCheckFeeRate")).json.pick
        ) and
        (__ \ Symbol("subscriptionFees") \ Symbol("premiseFee")).json.copyFrom(
          (__ \ Symbol("premiseFee")).json.pick
        ) and
        (__ \ Symbol("subscriptionFees") \ Symbol("premiseFeeRate")).json.copyFrom(
          (__ \ Symbol("premiseFeeRate")).json.pick
        ) and
        (__ \ Symbol("subscriptionFees") \ Symbol("totalFees")).json.copyFrom((__ \ Symbol("totalFees")).json.pick)
    ).reduce.orElse((__ \ Symbol("subscriptionFees")).json.pickBranch).orElse(Reads.pure(Json.obj()))

  implicit val reads: Reads[SubscriptionResponse] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "etmpFormBundleNumber").read[String] and
        (__ \ "amlsRefNo").read[String] and
        oldFeesStructureTransformer.andThen((__ \ "subscriptionFees").readNullable[SubscriptionFees]) and
        (__ \ "previouslySubmitted").readNullable[Boolean]
    ) apply SubscriptionResponse.apply _
  }

}
