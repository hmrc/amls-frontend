package models.estateagentbusiness

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import utils.TraversableValidators._

case class Services(services: Set[Service])

sealed trait Service

case object Commercial extends Service

case object Auction extends Service

case object Relocation extends Service

case object BusinessTransfer extends Service

case object AssetManagement extends Service

case object LandManagement extends Service

case object Development extends Service

case object SocialHousing extends Service

case object Residential extends Service

object Service {

  implicit val servicesFormRead = Rule[String, Service] {
    case "01" => Success(Residential)
    case "02" => Success(Commercial)
    case "03" => Success(Auction)
    case "04" => Success(Relocation)
    case "05" => Success(BusinessTransfer)
    case "06" => Success(AssetManagement)
    case "07" => Success(LandManagement)
    case "08" => Success(Development)
    case "09" => Success(SocialHousing)
    case _ =>
      Failure(Seq((Path \ "services") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val servicesFormWrite =
    Write[Service, String] {
      case Residential => "01"
      case Commercial => "02"
      case Auction => "03"
      case Relocation => "04"
      case BusinessTransfer => "05"
      case AssetManagement => "06"
      case LandManagement => "07"
      case Development => "08"
      case SocialHousing => "09"
    }

  implicit val jsonServiceReads: Reads[Service] =
    Reads {
      case JsString("01") => JsSuccess(Residential)
      case JsString("02") => JsSuccess(Commercial)
      case JsString("03") => JsSuccess(Auction)
      case JsString("04") => JsSuccess(Relocation)
      case JsString("05") => JsSuccess(BusinessTransfer)
      case JsString("06") => JsSuccess(AssetManagement)
      case JsString("07") => JsSuccess(LandManagement)
      case JsString("08") => JsSuccess(Development)
      case JsString("09") => JsSuccess(SocialHousing)
      case _ => JsError((JsPath \ "services") -> ValidationError("error.invalid"))
    }

  implicit val jsonServiceWrites =
    Writes[Service] {
      case Residential => JsString("01")
      case Commercial => JsString("02")
      case Auction => JsString("03")
      case Relocation => JsString("04")
      case BusinessTransfer => JsString("05")
      case AssetManagement => JsString("06")
      case LandManagement => JsString("07")
      case Development => JsString("08")
      case SocialHousing => JsString("09")
    }
}

object Services {
  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[Service]]
  ): Rule[UrlFormEncoded, Services] =
    From[UrlFormEncoded] { __ =>
       (__ \ "services").read(minLengthR[Set[Service]](1).withMessage("error.required.eab.business.services")) fmap Services.apply
  }

  implicit def formWrites
  (implicit
   w: Write[Service, String]
  ) = Write[Services, UrlFormEncoded] { data =>
    Map("services[]" -> data.services.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[Services]
}
