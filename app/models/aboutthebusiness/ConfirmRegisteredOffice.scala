package models.aboutthebusiness

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class ConfirmRegisteredOffice(isRegOfficeOrMainPlaceOfBusiness: Boolean)

object ConfirmRegisteredOffice {

  implicit val formats = Json.format[ConfirmRegisteredOffice]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ConfirmRegisteredOffice] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Boolean].withMessage("error.required.atb.confirm.office") map ConfirmRegisteredOffice.apply
    }

  implicit val formWrites: Write[ConfirmRegisteredOffice, UrlFormEncoded] =
    Write {
      case ConfirmRegisteredOffice(b) =>
        Map("isRegOfficeOrMainPlaceOfBusiness" -> Seq(b.toString))
    }
}
