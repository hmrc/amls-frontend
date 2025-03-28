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

import models.ResponseType.AmendOrVariationResponseType
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionReadyForReview, SubmissionStatus}
import play.api.libs.json._

import java.time.{Instant, LocalDateTime}
import java.time.ZoneOffset.UTC

sealed trait ResponseType

object ResponseType {

  import utils.MappingUtils.Implicits._

  case object SubscriptionResponseType extends ResponseType

  case object AmendOrVariationResponseType extends ResponseType

  implicit val jsonWrites: Writes[ResponseType] = Writes[ResponseType] {
    case SubscriptionResponseType     => JsString("SubscriptionReponse")
    case AmendOrVariationResponseType => JsString("AmendOrVariationResponse")
  }

  implicit val jsonReads: Reads[ResponseType] = {
    import play.api.libs.json.Reads.StringReads
    __.read[String] flatMap {
      case "SubscriptionReponse"      => SubscriptionResponseType
      case "AmendOrVariationResponse" => AmendOrVariationResponseType
      case _                          =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }
}

case class FeeResponse(
  responseType: ResponseType,
  amlsReferenceNumber: String,
  registrationFee: BigDecimal,
  fpFee: Option[BigDecimal],
  approvalCheckFee: Option[BigDecimal],
  premiseFee: BigDecimal,
  totalFees: BigDecimal,
  paymentReference: Option[String],
  difference: Option[BigDecimal],
  createdAt: LocalDateTime
) {

  def toPay(status: SubmissionStatus, submissionRequestStatus: Option[SubmissionRequestStatus] = None): BigDecimal = {
    val isRenewalAmendment: Boolean = submissionRequestStatus exists {
      _.isRenewalAmendment.getOrElse(false)
    }
    status match {
      case (RenewalSubmitted(_) | ReadyForRenewal(_)) if isRenewalAmendment         => difference.getOrElse(0)
      case SubmissionReadyForReview if responseType == AmendOrVariationResponseType => difference.getOrElse(0)
      case _                                                                        => totalFees
    }
  }

}

object FeeResponse {

  implicit val dateTimeRead: Reads[LocalDateTime] =
    (__ \ "$date")
      .read[Long]
      .map { dateTime =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime), UTC)
      }
      .orElse {
        (__ \ "$date" \ "$numberLong").read[Long].map { dateTime =>
          LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime), UTC)
        }
      }
      .orElse {
        (__ \ "$date" \ "$numberLong")
          .read[String]
          .map(dateTime => LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime.toLong), UTC))
      }

  implicit val dateTimeWrite: Writes[LocalDateTime] = (dateTime: LocalDateTime) =>
    Json.obj("$date" -> dateTime.atZone(UTC).toInstant.toEpochMilli)

  implicit val format: OFormat[FeeResponse] = Json.format[FeeResponse]
}
