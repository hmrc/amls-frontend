package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait PositionWithinBusiness

case object BeneficialOwner extends PositionWithinBusiness
case object Director extends PositionWithinBusiness
case object InternalAccountant extends PositionWithinBusiness
case object NominatedOfficer extends PositionWithinBusiness
case object Partner extends PositionWithinBusiness
case object SoleProprietor extends PositionWithinBusiness

object PositionWithinBusiness {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PositionWithinBusiness] =
    From[UrlFormEncoded] { readerURLFormEncoded =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (readerURLFormEncoded \ "positionWithinBusiness").read[String] flatMap {
        case "01" => BeneficialOwner
        case "02" => Director
        case "03" => InternalAccountant
        case "04" => NominatedOfficer
        case "05" => Partner
        case "06" => SoleProprietor
        case _ =>
          (Path \ "positionWithinBusiness") -> Seq(ValidationError("error.required.positionWithinBusiness"))
      }
    }

  implicit val formWrite: Write[PositionWithinBusiness, UrlFormEncoded] = Write {
    case BeneficialOwner => "positionWithinBusiness" -> "01"
    case Director => "positionWithinBusiness" -> "02"
    case InternalAccountant => "positionWithinBusiness" -> "03"
    case NominatedOfficer => "positionWithinBusiness" -> "04"
    case Partner => "positionWithinBusiness" -> "05"
    case SoleProprietor => "positionWithinBusiness" -> "06"
  }

  implicit val jsonReads: Reads[PositionWithinBusiness] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "positionWithinBusiness").read[String].flatMap[PositionWithinBusiness] {
      case "01" => BeneficialOwner
      case "02" => Director
      case "03" => InternalAccountant
      case "04" => NominatedOfficer
      case "05" => Partner
      case "06" => SoleProprietor
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[PositionWithinBusiness] {
    case BeneficialOwner => Json.obj("positionWithinBusiness" -> "01")
    case Director => Json.obj("positionWithinBusiness" -> "02")
    case InternalAccountant => Json.obj("positionWithinBusiness" -> "03")
    case NominatedOfficer => Json.obj("positionWithinBusiness" -> "04")
    case Partner => Json.obj("positionWithinBusiness" -> "05")
    case SoleProprietor => Json.obj("positionWithinBusiness" -> "06")
  }
}
