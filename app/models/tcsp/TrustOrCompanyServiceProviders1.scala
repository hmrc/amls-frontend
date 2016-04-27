package models.tcsp

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import utils.TraversableValidators._


case class TrustOrCompanyServiceProviders(services: Set[TrustOrCompanyServiceProvider])

sealed trait TrustOrCompanyServiceProvider

case object NomineeShareholdersProvider extends TrustOrCompanyServiceProvider

case object TrusteeProvider extends TrustOrCompanyServiceProvider

case object RegisteredOfficeEtc extends TrustOrCompanyServiceProvider

case object CompanyDirectorEtc extends TrustOrCompanyServiceProvider

object TrustOrCompanyServiceProvider {

  implicit val formRead = Rule[String, TrustOrCompanyServiceProvider] {
    case "01" => Success(NomineeShareholdersProvider)
    case "02" => Success(TrusteeProvider)
    case "03" => Success(RegisteredOfficeEtc)
    case "04" => Success(CompanyDirectorEtc)
    case _ =>
      Failure(Seq((Path \ "services") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val formWrite =  Write[TrustOrCompanyServiceProvider, String] {
    case NomineeShareholdersProvider => "01"
    case TrusteeProvider => "02"
    case RegisteredOfficeEtc => "03"
    case CompanyDirectorEtc => "04"
  }
  implicit val jsonServiceReads: Reads[TrustOrCompanyServiceProvider] =
    Reads {
      case JsString("01") => JsSuccess(NomineeShareholdersProvider)
      case JsString("02") => JsSuccess(TrusteeProvider)
      case JsString("03") => JsSuccess(RegisteredOfficeEtc)
      case JsString("04") => JsSuccess(CompanyDirectorEtc)
      case _ => JsError((JsPath \ "services") -> ValidationError("error.invalid"))
    }

  implicit val jsonServiceWrites =
    Writes[TrustOrCompanyServiceProvider] {
      case NomineeShareholdersProvider => JsString("01")
      case TrusteeProvider => JsString("02")
      case RegisteredOfficeEtc => JsString("03")
      case CompanyDirectorEtc => JsString("04")
    }
}

object TrustOrCompanyServiceProviders {
  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[TrustOrCompanyServiceProvider]]
  ): Rule[UrlFormEncoded, TrustOrCompanyServiceProviders] =
    From[UrlFormEncoded] { __ =>
      (__ \ "serviceProviders").read(
        minLength[Set[TrustOrCompanyServiceProvider]](1).withMessage("error.required.tcsp.service.providers")) fmap TrustOrCompanyServiceProviders.apply
    }

  implicit def formWrites
  (implicit
   w: Write[TrustOrCompanyServiceProvider, String]
  ) = Write[TrustOrCompanyServiceProviders, UrlFormEncoded] { data =>
    Map("serviceProviders[]" -> data.services.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[TrustOrCompanyServiceProviders]
}
