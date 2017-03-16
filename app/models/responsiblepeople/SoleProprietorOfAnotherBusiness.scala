package models.responsiblepeople

import jto.validation.forms._
import jto.validation.{ValidationError, From, Rule, Write}
import play.api.libs.json.Json

case class SoleProprietorOfAnotherBusiness (soleProprietorOfAnotherBusiness: Boolean)

object SoleProprietorOfAnotherBusiness {

  implicit val format =  Json.format[SoleProprietorOfAnotherBusiness]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, SoleProprietorOfAnotherBusiness] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "soleProprietorOfAnotherBusiness").read[Boolean].withError(ValidationError("error.required.rp.sole_proprietor", "personName")) map SoleProprietorOfAnotherBusiness.apply
    }

  implicit val formWrites = Write[SoleProprietorOfAnotherBusiness, UrlFormEncoded] {
    case SoleProprietorOfAnotherBusiness(value) => Map("soleProprietorOfAnotherBusiness" -> Seq(value.toString))
  }
}