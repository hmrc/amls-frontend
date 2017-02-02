package models

import org.joda.time.{DateTime, DateTimeZone}
import jto.validation.ValidationError
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
                       createdAt: DateTime)

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
