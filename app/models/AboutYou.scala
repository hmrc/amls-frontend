package models

import play.api.data.mapping.{To, Write, From, Rule}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Reads._
import play.api.libs.json.Writes
import play.api.libs.json.Writes._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class YourDetails(firstName: String, middleName: Option[String], lastName: String)

object YourDetails {

  implicit val formats = Json.format[YourDetails]

  implicit val formRule: Rule[UrlFormEncoded, YourDetails] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "firstname").read(minLength(1)) and
        (__ \ "middlename").read[Option[String]] and
        (__ \ "lastname").read(minLength(1))
      )(YourDetails.apply _)
  }

  implicit val formWrites: Write[YourDetails, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "firstname").write[String] and
        (__ \ "middlename").write[Option[String]] and
        (__ \ "lastname").write[String]
      )(unlift(YourDetails.unapply _))
  }
}

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
//
//  implicit val formRule: Rule[UrlFormEncoded, RoleWithinBusiness] = From[UrlFormEncoded] { __ =>
//    import play.api.data.mapping.forms.Rules._
//    (__ \ "roleWithinBusiness").read[String] flatMap {
//      case "01" => Rule.fromMapping { _ => Success(BeneficialShareholder) }
//      case "02" => Rule.fromMapping { _ => Success(Director) }
//      case "03" => Rule.fromMapping { _ => Success(ExternalAccountant) }
//      case "04" => Rule.fromMapping { _ => Success(InternalAccountant) }
//      case "05" => Rule.fromMapping { _ => Success(NominatedOfficer) }
//      case "06" => Rule.fromMapping { _ => Success(Partner) }
//      case "07" => Rule.fromMapping { _ => Success(SoleProprietor) }
//      case "08" => (__ \ "other").read(minLength(1))[String] flatMap {
//        value =>
//          Rule.fromMapping { _ => Success(Other(value)) }
//      }
//      case _ => Rule.fromMapping { _ => Failure(Seq(ValidationError("TODO"))) }
//    }
//  }
//
//  implicit def formWrites
//  (implicit w: Path => WriteLike[String, UrlFormEncoded]): Write[RoleWithinBusiness, UrlFormEncoded] = Write {
//    case BeneficialShareholder => w(Path \ "roleWithinBusiness").writes("01")
//    case Director => w(Path \ "roleWithinBusiness").writes("02")
//    case ExternalAccountant => w(Path \ "roleWithinBusiness").writes("03")
//    case InternalAccountant => w(Path \ "roleWithinBusiness").writes("04")
//    case NominatedOfficer => w(Path \ "roleWithinBusiness").writes("05")
//    case Partner => w(Path \ "roleWithinBusiness").writes("06")
//    case SoleProprietor => w(Path \ "roleWithinBusiness").writes("07")
//    case Other(value) =>
//      w(Path \ "roleWithinBusiness").writes("08") ++
//        w(Path \ "other").writes(value)
//  }

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

case class AboutYou(
                     yourDetails: Option[YourDetails] = None,
                     roleWithinBusiness: Option[RoleWithinBusiness] = None
                   ) {

  def yourDetails(v: YourDetails): AboutYou = {
    this.copy(yourDetails = Some(v))
  }

  def roleWithinBusiness(v: RoleWithinBusiness): AboutYou = {
    this.copy(roleWithinBusiness = Some(v))
  }
}

object AboutYou {

  val key = "about-you"

  implicit val reads: Reads[AboutYou] = (
    __.read[Option[YourDetails]] and
      __.read[Option[RoleWithinBusiness]]
    ) (AboutYou.apply _)

  implicit val writes: Writes[AboutYou] = Writes[AboutYou] {
    model =>
      Seq(
        Json.toJson(model.yourDetails).asOpt[JsObject],
        Json.toJson(model.roleWithinBusiness).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[AboutYou]): AboutYou =
    aboutYou.getOrElse(AboutYou())
}