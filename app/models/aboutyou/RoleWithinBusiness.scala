package models.aboutyou

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait RoleWithinBusiness

case object BeneficialShareholder extends RoleWithinBusiness
case object Director extends RoleWithinBusiness
case object ExternalAccountant extends RoleWithinBusiness
case object InternalAccountant extends RoleWithinBusiness
case object NominatedOfficer extends RoleWithinBusiness
case object Partner extends RoleWithinBusiness
case object SoleProprietor extends RoleWithinBusiness
case class Other(value: String) extends RoleWithinBusiness

object RoleWithinBusiness {

  implicit val formRule: Rule[UrlFormEncoded, RoleWithinBusiness] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "roleWithinBusiness").read[String] flatMap {
      case "01" => Rule.fromMapping { _ => Success(BeneficialShareholder) }
      case "02" => Rule.fromMapping { _ => Success(Director) }
      case "03" => Rule.fromMapping { _ => Success(ExternalAccountant) }
      case "04" => Rule.fromMapping { _ => Success(InternalAccountant) }
      case "05" => Rule.fromMapping { _ => Success(NominatedOfficer) }
      case "06" => Rule.fromMapping { _ => Success(Partner) }
      case "07" => Rule.fromMapping { _ => Success(SoleProprietor) }
      case "08" =>
        (__ \ "other").read(minLength(1)) flatMap {
          value =>
            Rule.fromMapping { _ => Success(Other(value)) }
        }
      case _ => Rule { _ =>
        Failure(Seq((Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))))
      }
    }
  }

  implicit val formWrites: Write[RoleWithinBusiness, UrlFormEncoded] = Write {
    case BeneficialShareholder => Map("roleWithinBusiness" -> Seq("01"))
    case Director => Map("roleWithinBusiness" -> Seq("02"))
    case ExternalAccountant => Map("roleWithinBusiness" -> Seq("03"))
    case InternalAccountant => Map("roleWithinBusiness" -> Seq("04"))
    case NominatedOfficer => Map("roleWithinBusiness" -> Seq("05"))
    case Partner => Map("roleWithinBusiness" -> Seq("06"))
    case SoleProprietor => Map("roleWithinBusiness" -> Seq("07"))
    case Other(value) =>
      Map(
        "roleWithinBusiness" -> Seq("08"),
        "other" -> Seq(value)
      )
  }

  implicit val jsonReads =
    (__ \ "roleWithinBusiness").read[String] flatMap[RoleWithinBusiness] {
      case "01" => Reads(_ => JsSuccess(BeneficialShareholder))
      case "02" => Reads(_ => JsSuccess(Director))
      case "03" => Reads(_ => JsSuccess(ExternalAccountant))
      case "04" => Reads(_ => JsSuccess(InternalAccountant))
      case "05" => Reads(_ => JsSuccess(NominatedOfficer))
      case "06" => Reads(_ => JsSuccess(Partner))
      case "07" => Reads(_ => JsSuccess(SoleProprietor))
      case "08" => (__ \ "roleWithinBusinessOther").read[String] map {
        Other(_)
      }
    }

  implicit val jsonWrites = Writes[RoleWithinBusiness] {
    case BeneficialShareholder => Json.obj("roleWithinBusiness" -> "01")
    case Director => Json.obj("roleWithinBusiness" -> "02")
    case ExternalAccountant => Json.obj("roleWithinBusiness" -> "03")
    case InternalAccountant => Json.obj("roleWithinBusiness" -> "04")
    case NominatedOfficer => Json.obj("roleWithinBusiness" -> "05")
    case Partner => Json.obj("roleWithinBusiness" -> "06")
    case SoleProprietor => Json.obj("roleWithinBusiness" -> "07")
    case Other(value) => Json.obj(
      "roleWithinBusiness" -> "08",
      "roleWithinBusinessOther" -> value
    )
  }
}