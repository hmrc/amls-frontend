package models.responsiblepeople

import models.Country
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait Nationality

case object British extends Nationality

case object Irish extends Nationality

case class OtherCountry(name: Country) extends Nationality

object Nationality {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, Nationality] =
    From[UrlFormEncoded] { readerURLFormEncoded =>
      import play.api.data.mapping.forms.Rules._
      (readerURLFormEncoded \ "nationality").read[String].withMessage("error.required.nationality") flatMap {
        case "01" => British
        case "02" => Irish
        case "03" =>
          (readerURLFormEncoded \ "otherCountry").read[Country] fmap OtherCountry.apply
        case _ =>
          (Path \ "nationality") -> Seq(ValidationError("error.invalid"))
      }
    }

  implicit val formWrite: Write[Nationality, UrlFormEncoded] = Write {
    case British => "nationality" -> "01"
    case Irish => "nationality" -> "02"
    case OtherCountry(value) => Map("nationality" -> "03",
      "otherCountry" -> value.code)
  }

  implicit val jsonReads: Reads[Nationality] = {
    import play.api.libs.json._

    (__ \ "nationality").read[String].flatMap[Nationality] {
      case "01" => British
      case "02" => Irish
      case "03" => (JsPath \ "otherCountry").read[Country] map OtherCountry.apply
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[Nationality] {
    case British => Json.obj("nationality" -> "01")
    case Irish => Json.obj("nationality" -> "02")
    case OtherCountry(value) => Json.obj(
      "nationality" -> "03",
      "otherCountry" -> value
    )
  }

  implicit def getNationality(country: Option[Country]): Option[Nationality] = {
    country match {
      case Some(countryType)=> Some(countryType)
      case _ => None
    }
  }

  implicit def getNationality(country: Country): Nationality = {
    country match {
      case Country("United Kingdom", "GB") => British
      case Country("Ireland", "IE") => Irish
      case someCountry => OtherCountry(someCountry)
    }
  }

  implicit def getCountry(nationality: Nationality): Country = {
    nationality match {
      case British =>Country("United Kingdom", "GB")
      case Irish => Country("Ireland", "IE")
      case OtherCountry(someCountry) => someCountry
    }
  }
}
