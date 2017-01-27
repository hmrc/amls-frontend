package models.responsiblepeople

import models.estateagentbusiness.Other
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait PassportType

case class UKPassport(passportNumberUk: String) extends PassportType
case class NonUKPassport(passportNumberNonUk: String) extends PassportType
case object NoPassport extends PassportType

object PassportType {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PassportType] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "passportType").read[String].withMessage("error.required.rp.passport.option") flatMap {
      case "01" =>
        (__ \ "ukPassportNumber").read(ukPassportType) map UKPassport.apply
      case "02" =>
        (__ \ "nonUKPassportNumber").read(noUKPassportType) map NonUKPassport.apply
      case "03" => NoPassport
      case _ =>
        (Path \ "passportType") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[PassportType, UrlFormEncoded] = Write {
    case UKPassport(ukNumber) =>  Map(
      "passportType" -> "01",
      "ukPassportNumber" -> ukNumber
    )
    case NonUKPassport(nonUKNumber) =>   Map(
      "passportType" -> "02",
      "nonUKPassportNumber" -> nonUKNumber
    )
    case NoPassport => "passportType" -> "03"
  }

  implicit val jsonReads : Reads[PassportType] = {
    import play.api.libs.json.Reads.StringReads
      (__ \ "passportType").read[String].flatMap[PassportType] {
        case "01" =>
          (__ \ "ukPassportNumber").read[String] map {
            UKPassport(_)
          }
        case "02" =>
          (__ \ "nonUKPassportNumber").read[String] map {
            NonUKPassport(_)
          }
        case "03" => NoPassport
        case _ =>
          play.api.data.validation.ValidationError("error.invalid")
      }
  }

  implicit val jsonWrites = Writes[PassportType] {
    case UKPassport(ukNumber) =>  Json.obj(
      "passportType" -> "01",
      "ukPassportNumber" -> ukNumber
    )
    case NonUKPassport(nonUKNumber) =>  Json.obj(
      "passportType" -> "02",
      "nonUKPassportNumber" -> nonUKNumber
    )
    case NoPassport => Json.obj("passportType" -> "03")
  }
}

