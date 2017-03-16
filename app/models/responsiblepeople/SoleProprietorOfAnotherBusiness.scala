package models.responsiblepeople

import cats.data.Validated.Valid
import jto.validation.{ValidationError, From, Rule, Write}
import play.api.libs.json.Json
import jto.validation._
import jto.validation.forms.UrlFormEncoded

case class SoleProprietorOfAnotherBusiness (soleProprietorOfAnotherBusiness: Boolean)

object SoleProprietorOfAnotherBusiness {

  implicit val format =  Json.format[SoleProprietorOfAnotherBusiness]
  import utils.MappingUtils.Implicits._

  private val soleProprietorWithNameMapping = Rule.fromMapping[(String, Option[Boolean]), SoleProprietorOfAnotherBusiness] {
    case (name, Some(response)) => Valid(SoleProprietorOfAnotherBusiness(response))
    case (name, None) => Invalid(Seq(ValidationError("error.required.rp.sole_proprietor", name)))
  }

  implicit val formRule: Rule[UrlFormEncoded, SoleProprietorOfAnotherBusiness] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      ((__ \ "personName").read[String] ~
      (__ \ "soleProprietorOfAnotherBusiness").read[Option[Boolean]]).tupled
        .andThen(soleProprietorWithNameMapping).repath(_ => Path \ "soleProprietorOfAnotherBusiness")
    }

  implicit val formWrites = Write[SoleProprietorOfAnotherBusiness, UrlFormEncoded] {
    case SoleProprietorOfAnotherBusiness(value) => Map("soleProprietorOfAnotherBusiness" -> Seq(value.toString))
  }
}