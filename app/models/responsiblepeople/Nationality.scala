package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait Nationality

case object British extends Nationality

case object Irish extends Nationality

case class OtherCountry(value: String) extends Nationality

object Nationality {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, Nationality] =
    From[UrlFormEncoded] { readerURLFormEncoded =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (readerURLFormEncoded \ "nationality").read[String] flatMap {
        case "01" => British
        case "02" => Irish
        case "08" =>
          (readerURLFormEncoded \ "roleWithinBusinessOther").read(roleWithinBusinessOtherType) fmap OtherCountry.apply
        case _ =>
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
      }
    }

  implicit val formWrite: Write[Nationality, UrlFormEncoded] = Write {
    case British => "nationality" -> "01"
    case Irish => "nationality" -> "02"
    case OtherCountry(value) => Map("nationality" -> "08",
      "OtherCountry" -> value)
  }

  implicit val jsonReads: Reads[Nationality] = {
    import play.api.libs.json._

    (__ \ "roleWithinBusiness").read[String].flatMap[Nationality] {
      case "01" => British
      case "02" => Irish
      case "03" => (JsPath \ "nationality").read[String] map {
        OtherCountry(_)
      }
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[Nationality] {
    case British => Json.obj("nationality" -> "01")
    case Irish => Json.obj("nationality" -> "02")
    case OtherCountry(value) => Json.obj(
      "nationality" -> "08",
      "OtherCountry" -> value
    )
  }
}
