package models.businessmatching

import jto.validation.forms.Rules._
import jto.validation.{Write, From, Rule}
import jto.validation.forms._
import play.api.libs.json.Json

case class CompanyRegistrationNumber(companyRegistrationNumber: String)


object CompanyRegistrationNumber {

  import utils.MappingUtils.Implicits._

  val registrationNumberRegex = "^[A-Z0-9]{8}$".r
  val registrationType = notEmpty.withMessage("error.required.bm.registration.number") compose
    pattern(registrationNumberRegex).withMessage("error.invalid.bm.registration.number")

  implicit val formats = Json.format[CompanyRegistrationNumber]

  implicit val formReads: Rule[UrlFormEncoded, CompanyRegistrationNumber] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "companyRegistrationNumber").read(registrationType) fmap CompanyRegistrationNumber.apply
  }

  implicit val formWrites: Write[CompanyRegistrationNumber, UrlFormEncoded] = Write {
    case CompanyRegistrationNumber(registered) => Map("companyRegistrationNumber" -> Seq(registered))
  }
}
