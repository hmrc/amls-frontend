package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._
import utils.TraversableValidators._

case class Positions(positions: Set[PositionWithinBusiness]) {

  def personalTax = this.positions.exists(a => a == SoleProprietor || a == Partner)
}

sealed trait PositionWithinBusiness

case object BeneficialOwner extends PositionWithinBusiness
case object Director extends PositionWithinBusiness
case object InternalAccountant extends PositionWithinBusiness
case object NominatedOfficer extends PositionWithinBusiness
case object Partner extends PositionWithinBusiness
case object SoleProprietor extends PositionWithinBusiness

object PositionWithinBusiness {

  import utils.MappingUtils.Implicits._

  implicit val formRule = Rule[String, PositionWithinBusiness] {
    case "01" => Success(BeneficialOwner)
    case "02" => Success(Director)
    case "03" => Success(InternalAccountant)
    case "04" => Success(NominatedOfficer)
    case "05" => Success(Partner)
    case "06" => Success(SoleProprietor)
    case _ =>
      Failure(Seq((Path \ "positions") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val formWrite = Write[PositionWithinBusiness, String] {
      case BeneficialOwner => "01"
      case Director => "02"
      case InternalAccountant => "03"
      case NominatedOfficer => "04"
      case Partner => "05"
      case SoleProprietor => "06"
  }

  implicit val jsonReads: Reads[PositionWithinBusiness] =
    Reads {
      case JsString("01") => JsSuccess(BeneficialOwner)
      case JsString("02") => JsSuccess(Director)
      case JsString("03") => JsSuccess(InternalAccountant)
      case JsString("04") => JsSuccess(NominatedOfficer)
      case JsString("05") => JsSuccess(Partner)
      case JsString("06") => JsSuccess(SoleProprietor)
      case _ => JsError((JsPath \ "positions") -> ValidationError("error.invalid"))
    }

  implicit val jsonWrites = Writes[PositionWithinBusiness] {
    case BeneficialOwner => JsString("01")
    case Director => JsString("02")
    case InternalAccountant => JsString("03")
    case NominatedOfficer => JsString("04")
    case Partner => JsString("05")
    case SoleProprietor => JsString("06")
  }
}

object Positions {
  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[PositionWithinBusiness]]
  ): Rule[UrlFormEncoded, Positions] =
    From[UrlFormEncoded] { __ =>
      (__ \ "positions")
        .read(minLength[Set[PositionWithinBusiness]](1)
        .withMessage("error.required.positionWithinBusiness")) fmap Positions.apply
    }

  implicit def formWrites
  (implicit
   w: Write[PositionWithinBusiness, String]
  ) = Write[Positions, UrlFormEncoded] { data =>
    Map("positions[]" -> data.positions.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[Positions]
}
