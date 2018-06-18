/*
 * Copyright 2018 HM Revenue & Customs
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

import models.ResponseType.SubscriptionResponseType
import models.status.{SubmissionReadyForReview, SubmissionStatus}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._

sealed trait ResponseType

object ResponseType {

  import utils.MappingUtils.Implicits._

  case object SubscriptionResponseType extends ResponseType
  case object AmendOrVariationResponseType extends ResponseType

  implicit val jsonWrites = Writes[ResponseType] {
    case SubscriptionResponseType => JsString("SubscriptionReponse")
    case AmendOrVariationResponseType => JsString("AmendOrVariationResponse")
  }

  implicit val jsonReads : Reads[ResponseType] = {
    import play.api.libs.json.Reads.StringReads
    __.read[String] flatMap {
      case "SubscriptionReponse" => SubscriptionResponseType
      case "AmendOrVariationResponse" => AmendOrVariationResponseType
      case _ =>
        play.api.data.validation.ValidationError("error.invalid")
    }
  }
}

case class FeeResponse(responseType: ResponseType,
                       amlsReferenceNumber: String,
                       registrationFee: BigDecimal,
                       fpFee: Option[BigDecimal],
                       premiseFee: BigDecimal,
                       totalFees: BigDecimal,
                       paymentReference: Option[String],
                       difference: Option[BigDecimal],
                       createdAt: DateTime) {

  def toPay(status: SubmissionStatus): BigDecimal = status match {
    case SubmissionReadyForReview if responseType == SubscriptionResponseType => difference.getOrElse(0)
    case _ => totalFees
  }

}

object FeeResponse {

  implicit val dateTimeRead: Reads[DateTime] =
    (__ \ "$date").read[Long].map { dateTime =>
      new DateTime(dateTime, DateTimeZone.UTC)
    }


  implicit val dateTimeWrite: Writes[DateTime] = new Writes[DateTime] {
    def writes(dateTime: DateTime): JsValue = Json.obj(
      "$date" -> dateTime.getMillis
    )
  }
  implicit val format = Json.format[FeeResponse]
}
