package models.aboutthebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{Path, From, Rule, Write}
import play.api.data.validation.ValidationError
import play.api.libs.json.Json

case class ConfirmRegisteredOffice(isRegOfficeOrMainPlaceOfBusiness: Boolean)

object ConfirmRegisteredOffice {

  implicit val formats = Json.format[ConfirmRegisteredOffice]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ConfirmRegisteredOffice] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Option[Boolean]] flatMap {
        case Some(data) => ConfirmRegisteredOffice(data)
        case _ => (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required.atb.confirm.office"))
      }
    }

  implicit val formWrites: Write[ConfirmRegisteredOffice, UrlFormEncoded] =
    Write {
      case ConfirmRegisteredOffice(b) =>
        Map("isRegOfficeOrMainPlaceOfBusiness" -> Seq(b.toString))
    }
}