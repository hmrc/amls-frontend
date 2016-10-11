package models

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.ISODateTimeFormat
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait ResponseType

case object SubscriptionResponseType extends ResponseType
case object AmendOrVariationResponseType extends ResponseType

object ResponseType {

  import utils.MappingUtils.Implicits._

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
        ValidationError("error.invalid")
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
                       createdAt: DateTime)

object FeeResponse {
  implicit def convert(subscriptionResponse: SubscriptionResponse): FeeResponse = {
    FeeResponse(SubscriptionResponseType,
      subscriptionResponse.amlsRefNo,
      subscriptionResponse.registrationFee,
      subscriptionResponse.fpFee,
      subscriptionResponse.premiseFee,
      subscriptionResponse.totalFees,
      Some(subscriptionResponse.paymentReference),
      None,
      DateTime.now(DateTimeZone.UTC))
  }

  implicit def convert2(amendVariationResponse: AmendVariationResponse,  amlsReferenceNumber: String): FeeResponse = {
    FeeResponse(AmendOrVariationResponseType,
      amlsReferenceNumber,
      amendVariationResponse.registrationFee,
      amendVariationResponse.fpFee,
      amendVariationResponse.premiseFee,
      amendVariationResponse.totalFees,
      amendVariationResponse.paymentReference,
      amendVariationResponse.difference,
      DateTime.now(DateTimeZone.UTC))
  }

  val dateTimeFormat = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC

  implicit val format = Json.format[FeeResponse]
}
