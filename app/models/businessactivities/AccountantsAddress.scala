package models.businessactivities

import models.Country
import models.FormTypes._
import play.api.data.mapping.{From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json.{Writes, Reads}

sealed trait AccountantsAddress {
  def toLines: Seq[String] = this match {
    case a: UkAccountantsAddress =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: NonUkAccountantsAddress =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.country.toString)
      ).flatten
  }
}

case class UkAccountantsAddress(
                                 addressLine1: String,
                                 addressLine2: String,
                                 addressLine3: Option[String],
                                 addressLine4: Option[String],
                                 postCode: String
                               ) extends AccountantsAddress

case class NonUkAccountantsAddress(
                                    addressLine1: String,
                                    addressLine2: String,
                                    addressLine3: Option[String],
                                    addressLine4: Option[String],
                                    country: Country
                                  ) extends AccountantsAddress


object AccountantsAddress {

  implicit val formRule: Rule[UrlFormEncoded, AccountantsAddress] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import utils.MappingUtils.Implicits._
    (__ \ "isUK").read[Boolean].withMessage("error.required.uk.or.overseas") flatMap {
      case true =>
        (
          (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) and
            (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) and
            (__ \ "addressLine3").read(optionR(validateAddress)) and
            (__ \ "addressLine4").read(optionR(validateAddress)) and
            (__ \ "postCode").read(postcodeType)
          ) (UkAccountantsAddress.apply _)
      case false =>
        (
          (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) and
            (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) and
            (__ \ "addressLine3").read(optionR(validateAddress)) and
            (__ \ "addressLine4").read(optionR(validateAddress)) and
            (__ \ "country").read[Country]
          ) (NonUkAccountantsAddress.apply _)
    }
  }

  implicit val jsonReads: Reads[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "accountantsAddressPostCode").read[String] andKeep (
        ((__ \ "accountantsAddressLine1").read[String] and
        (__ \ "accountantsAddressLine2").read[String] and
        (__ \ "accountantsAddressLine3").readNullable[String] and
        (__ \ "accountantsAddressLine4").readNullable[String] and
        (__ \ "accountantsAddressPostCode").read[String])  (UkAccountantsAddress.apply _) map identity[AccountantsAddress]
      ) orElse
        ( (__ \ "accountantsAddressLine1").read[String] and
          (__ \ "accountantsAddressLine2").read[String] and
          (__ \ "accountantsAddressLine3").readNullable[String] and
          (__ \ "accountantsAddressLine4").readNullable[String] and
          (__ \ "accountantsAddressCountry").read[Country]) (NonUkAccountantsAddress.apply _)

  }

  implicit val jsonWrites: Writes[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[AccountantsAddress] {
      case a: UkAccountantsAddress =>
        (
            (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").write[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressPostCode").write[String]
          ) (unlift(UkAccountantsAddress.unapply)).writes(a)
      case a: NonUkAccountantsAddress =>
        (
            (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").write[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressCountry").write[Country]
          ) (unlift(NonUkAccountantsAddress.unapply)).writes(a)
    }
  }
}
