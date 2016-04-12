package models.responsiblepeople

import models.Country
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{Path, From, Rule, Write}
import play.api.libs.json.{Reads, Writes}

sealed trait PersonAddress {

  def toLines: Seq[String] = this match {
    case a: UKAddress =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: NonUKAddress =>
      Seq(
        Some(a.addressLineNonUK1),
        Some(a.addressLineNonUK2),
        a.addressLineNonUK3,
        a.addressLineNonUK4,
        Some(a.country.toString)
      ).flatten
  }
}

case class UKAddress(
                      addressLine1: String,
                      addressLine2: String,
                      addressLine3: Option[String],
                      addressLine4: Option[String],
                      postCode: String) extends PersonAddress

case class NonUKAddress(
                         addressLineNonUK1: String,
                         addressLineNonUK2: String,
                         addressLineNonUK3: Option[String],
                         addressLineNonUK4: Option[String],
                         country: Country) extends PersonAddress

object PersonAddress {
  implicit val formRule: Rule[UrlFormEncoded, PersonAddress] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      import models.FormTypes._
      import utils.MappingUtils.Implicits._

      (__ \ "isUK").read[Boolean].withMessage("error.required.uk.or.overseas") flatMap {
        case true => (
            (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) ~
            (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) ~
            (__ \ "addressLine3").read(optionR(validateAddress)) ~
            (__ \ "addressLine4").read(optionR(validateAddress)) ~
            (__ \ "postCode").read(postcodeType)
          )(UKAddress.apply _)
        case false => (
            (__ \ "addressLineNonUK1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) ~
            (__ \ "addressLineNonUK2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) ~
            (__ \ "addressLineNonUK3").read(optionR(validateAddress)) ~
            (__ \ "addressLineNonUK4").read(optionR(validateAddress)) ~
            (__ \ "country").read[Country]
          )(NonUKAddress.apply _)
      }
    }

  implicit val formWrites = Write[PersonAddress, UrlFormEncoded] {
    case a: UKAddress =>
      Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq(a.addressLine1),
        "addressLine2" -> Seq(a.addressLine2),
        "addressLine3" -> a.addressLine3.toSeq,
        "addressLine4" -> a.addressLine4.toSeq,
        "postCode" -> Seq(a.postCode)
      )
    case a: NonUKAddress =>
      Map(
        "isUK" -> Seq("false"),
        "addressLineNonUK1" -> Seq(a.addressLineNonUK1),
        "addressLineNonUK2" -> Seq(a.addressLineNonUK2),
        "addressLineNonUK3" -> a.addressLineNonUK3.toSeq,
        "addressLineNonUK4" -> a.addressLineNonUK4.toSeq,
        "country" -> Seq(a.country.code)
      )
  }

  implicit val jsonReads: Reads[PersonAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "personAddressPostCode").read[String] andKeep (
      (
        (__ \ "personAddressLine1").read[String] and
        (__ \ "personAddressLine2").read[String] and
        (__ \ "personAddressLine3").readNullable[String] and
        (__ \ "personAddressLine4").readNullable[String] and
        (__ \ "personAddressPostCode").read[String])(UKAddress.apply _) map identity[PersonAddress]
      ) orElse
      (
        (__ \ "personAddressLine1").read[String] and
        (__ \ "personAddressLine2").read[String] and
        (__ \ "personAddressLine3").readNullable[String] and
        (__ \ "personAddressLine4").readNullable[String] and
        (__ \ "personAddressCountry").read[Country])(NonUKAddress.apply _)
  }

  implicit val jsonWrites: Writes[PersonAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[PersonAddress] {
      case a: UKAddress =>
        (
            (__ \ "personAddressLine1").write[String] and
            (__ \ "personAddressLine2").write[String] and
            (__ \ "personAddressLine3").writeNullable[String] and
            (__ \ "personAddressLine4").writeNullable[String] and
            (__ \ "personAddressPostCode").write[String]
          )(unlift(UKAddress.unapply)).writes(a)
      case a: NonUKAddress =>
        (
            (__ \ "personAddressLine1").write[String] and
            (__ \ "personAddressLine2").write[String] and
            (__ \ "personAddressLine3").writeNullable[String] and
            (__ \ "personAddressLine4").writeNullable[String] and
            (__ \ "personAddressCountry").write[Country]
          )(unlift(NonUKAddress.unapply)).writes(a)
    }
  }
}
