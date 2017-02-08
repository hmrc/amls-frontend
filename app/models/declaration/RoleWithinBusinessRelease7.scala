package models.declaration.release7

import models.FormTypes._
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json.{JsError, _}
import play.api.libs.json.Reads.StringReads
import play.api.data.validation.{ValidationError => JsonValidationError}
import jto.validation.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR
import cats.data.Validated.{Invalid, Valid}
//import models.declaration.Other

case class RoleWithinBusinessRelease7(items: Set[RoleType]) {
  def sorted = {
    items.toSeq.sortBy( it => it.value)
  }
}

sealed trait RoleType {
  val value: String =
    this match {
      case BeneficialShareholder => "BeneficialShareholder"
      case Director => "Director"
      case Partner => "Partner"
      case InternalAccountant => "InternalAccountant"
      case ExternalAccountant => "ExternalAccountant"
      case SoleProprietor => "SoleProprietor"
      case NominatedOfficer => "NominatedOfficer"
      case DesignatedMember => "DesignatedMember"
      case Other(_) => "Other"
    }
}

case object BeneficialShareholder extends RoleType
case object Director extends RoleType
case object Partner extends RoleType
case object InternalAccountant extends RoleType
case object ExternalAccountant extends RoleType
case object SoleProprietor extends RoleType
case object NominatedOfficer extends RoleType
case object DesignatedMember extends RoleType
case class Other(details:String) extends RoleType

object RoleWithinBusinessRelease7 {

  import utils.MappingUtils.Implicits._

  val maxDetailsLength = 255
  val otherDetailsType = notEmptyStrip andThen
    notEmpty.withMessage("error.required") andThen
    maxLength(maxDetailsLength).withMessage("error.invalid.maxlength.255")

  implicit val formRule: Rule[UrlFormEncoded, RoleWithinBusinessRelease7] =
    From[UrlFormEncoded] { readerURLFormEncoded =>
      (readerURLFormEncoded \ "roleWithinBusiness").read(minLengthR[Set[String]](1).withMessage("error.required")) flatMap { z =>
        z.map {
          case "BeneficialShareholder" => Rule[UrlFormEncoded, RoleType](_ => Valid(BeneficialShareholder))
          case "Director" => Rule[UrlFormEncoded, RoleType](_ => Valid(Director))
          case "Partner" => Rule[UrlFormEncoded, RoleType](_ => Valid(Partner))
          case "InternalAccountant" => Rule[UrlFormEncoded, RoleType](_ => Valid(InternalAccountant))
          case "ExternalAccountant" => Rule[UrlFormEncoded, RoleType](_ => Valid(ExternalAccountant))
          case "SoleProprietor" => Rule[UrlFormEncoded, RoleType](_ => Valid(SoleProprietor))
          case "NominatedOfficer" => Rule[UrlFormEncoded, RoleType](_ => Valid(NominatedOfficer))
          case "DesignatedMember" => Rule[UrlFormEncoded, RoleType](_ => Valid(DesignatedMember))
          case "Other" =>
            (readerURLFormEncoded \ "roleWithinBusinessOther").read(otherDetailsType) map Other.apply
          case _ =>
            Rule[UrlFormEncoded, RoleType] { _ =>
              Invalid(Seq((Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))))
            }
        }.foldLeft[Rule[UrlFormEncoded, Set[RoleType]]](
          Rule[UrlFormEncoded, Set[RoleType]](_ => Valid(Set.empty))
        ) {
          case (m, n) =>
            n flatMap { x =>
              m map {
                _ + x
              }
            }
        } map RoleWithinBusinessRelease7.apply
        }
      }

  implicit def formWrites = Write[RoleWithinBusinessRelease7, UrlFormEncoded] {
    case RoleWithinBusinessRelease7(transactions) =>
      Map(
        "roleWithinBusiness[]" -> (transactions map {
          _.value
        }).toSeq
      ) ++ transactions.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, Other(name)) =>
          m ++ Map("roleWithinBusinessOther" -> Seq(name))
        case (m, _) =>
          m
      }
  }

  val businessRolePathName = "roleWithinBusiness"
  val businessRolePath = JsPath \ businessRolePathName

  val preRelease7JsonRead =  businessRolePath.read[String].flatMap[Set[RoleType]] {
    case "01" => Reads(_ => JsSuccess(Set(BeneficialShareholder)))
    case "02" => Reads(_ => JsSuccess(Set(Director)))
    case "03" => Reads(_ => JsSuccess(Set(ExternalAccountant)))
    case "04" => Reads(_ => JsSuccess(Set(InternalAccountant)))
    case "05" => Reads(_ => JsSuccess(Set(NominatedOfficer)))
    case "06" => Reads(_ => JsSuccess(Set(Partner)))
    case "07" => Reads(_ => JsSuccess(Set(SoleProprietor)))
    case "08" => (JsPath \ "roleWithinBusinessOther").read[String] map { x =>
      Set(Other(x))
    }
    case _ => play.api.data.validation.ValidationError("error.invalid")
  }

  val fallback = Reads(x => (x \ businessRolePathName).getOrElse(JsNull) match {
      case JsNull => JsError(businessRolePath -> JsonValidationError("error.path.missing"))
      case _ => JsError(businessRolePath -> JsonValidationError("error.invalid"))
    }) map identity[Set[RoleType]]

  implicit val jsonReads: Reads[RoleWithinBusinessRelease7] =
    (__ \ "roleWithinBusiness").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "BeneficialShareholder" => Reads(_ => JsSuccess(BeneficialShareholder)) map identity[RoleType]
        case "Director" => Reads(_ => JsSuccess(Director)) map identity[RoleType]
        case "Partner" => Reads(_ => JsSuccess(Partner)) map identity[RoleType]
        case "InternalAccountant" => Reads(_ => JsSuccess(InternalAccountant)) map identity[RoleType]
        case "ExternalAccountant" => Reads(_ => JsSuccess(ExternalAccountant)) map identity[RoleType]
        case "SoleProprietor" => Reads(_ => JsSuccess(SoleProprietor)) map identity[RoleType]
        case "NominatedOfficer" => Reads(_ => JsSuccess(NominatedOfficer)) map identity[RoleType]
        case "DesignatedMember" => Reads(_ => JsSuccess(DesignatedMember)) map identity[RoleType]
        case "Other" =>
          (JsPath \ "roleWithinBusinessOther").read[String].map(Other.apply) map identity[RoleType]
        case _ =>
          Reads(_ => JsError(businessRolePath -> JsonValidationError("error.invalid")))
      }.foldLeft[Reads[Set[RoleType]]](
        Reads[Set[RoleType]](_ => JsSuccess(Set.empty))
      ) {
        (result, data) =>
          data flatMap { m =>
            result.map { n =>
              n + m
            }
          }
      }
    }.orElse(preRelease7JsonRead).orElse(fallback)
      .map(RoleWithinBusinessRelease7.apply)

  implicit val jsonWrite = Writes[RoleWithinBusinessRelease7] {
    case RoleWithinBusinessRelease7(transactions) =>
      Json.obj(
        businessRolePathName -> (transactions map {
          _.value
        }).toSeq
      ) ++ transactions.foldLeft[JsObject](Json.obj()) {
        case (m, Other(name)) =>
          m ++ Json.obj("roleWithinBusinessOther" -> name)
        case (m, _) =>
          m
      }
  }
}
