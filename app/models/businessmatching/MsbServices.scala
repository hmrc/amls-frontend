package models.businessmatching

import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json._
import cats.data.Validated.{Valid, Invalid}
import utils.TraversableValidators._

case class MsbServices(msbServices : Set[MsbService])

sealed trait MsbService

case object TransmittingMoney extends MsbService
case object CurrencyExchange extends MsbService
case object ChequeCashingNotScrapMetal extends MsbService
case object ChequeCashingScrapMetal extends MsbService

object MsbService {

  implicit val serviceR = Rule[String, MsbService] {
    case "01" => Valid(TransmittingMoney)
    case "02" => Valid(CurrencyExchange)
    case "03" => Valid(ChequeCashingNotScrapMetal)
    case "04" => Valid(ChequeCashingScrapMetal)
    case _ => Invalid(Seq(Path -> Seq(ValidationError("error.invalid"))))
  }

  implicit val serviceW = Write[MsbService, String] {
    case TransmittingMoney => "01"
    case CurrencyExchange => "02"
    case ChequeCashingNotScrapMetal => "03"
    case ChequeCashingScrapMetal => "04"
  }

  implicit val jsonR:Reads[MsbService] =  Reads {
    case JsString("01") => JsSuccess(TransmittingMoney)
    case JsString("02") => JsSuccess(CurrencyExchange)
    case JsString("03") => JsSuccess(ChequeCashingNotScrapMetal)
    case JsString("04") => JsSuccess(ChequeCashingScrapMetal)
    case _ => JsError((JsPath \ "services") -> play.api.data.validation.ValidationError("error.invalid"))
  }

  implicit val jsonW = Writes[MsbService] {
    case TransmittingMoney => JsString("01")
    case CurrencyExchange => JsString("02")
    case ChequeCashingNotScrapMetal => JsString("03")
    case ChequeCashingScrapMetal => JsString("04")
  }
}

object MsbServices {

  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[MsbService]]
  ): Rule[UrlFormEncoded, MsbServices] =
    From[UrlFormEncoded] { __ =>
      (__ \ "msbServices").read(minLengthR[Set[MsbService]](1).withMessage("error.required.msb.services")).flatMap(MsbServices.apply)
    }

  implicit def formWrites
  (implicit
   w: Write[MsbService, String]
  ) = Write[MsbServices, UrlFormEncoded] { data =>
    Map("msbServices[]" -> data.msbServices.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[MsbServices]
}

