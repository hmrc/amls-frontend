package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait PassportType

case class UKPassport(passportNumberUk: String) extends PassportType
case class NonUKPassport(passportNumberNonUk: String) extends PassportType

case object NoPassport extends PassportType

object PassportType {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PassportType] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "passportType").read[String] flatMap {
      case "01" =>
        (__ \ "passportNumberUk").read(descriptionType) fmap UKPassport.apply
      case "02" =>
        (__ \ "passportNumberNonUk").read(descriptionType) fmap NonUKPassport.apply
      case "03" => NoPassport
      case _ =>
        (Path \ "passportType") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[PassportType, UrlFormEncoded] = Write {
    case UKPassport(ukNumber) =>  Map(
      "passportType" -> "01",
      "passportNumberUk" -> ukNumber
    )
    case NonUKPassport(nonUKNumber) =>   Map(
      "passportType" -> "02",
      "passportNumberNonUk" -> nonUKNumber
    )
    case NoPassport => "passportType" -> "03"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "passportType").read[String].flatMap[PassportType] {
      case "01" =>
        (JsPath \ "passportType").read[String] map {
          UKPassport(_)
        }
      case "02" =>
        (JsPath \ "passportType").read[String] map {
          NonUKPassport(_)
        }
      case "03" => NoPassport
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[PassportType] {
    case UKPassport(ukNumber) =>  Json.obj(
      "passportType" -> "01",
      "passportNumberUk" -> ukNumber
    )
    case NonUKPassport(nonUKNumber) =>  Json.obj(
      "passportType" -> "02",
      "passportNumberNonUk" -> nonUKNumber
    )
    case NoPassport => Json.obj("passportType" -> "03")
  }
}

