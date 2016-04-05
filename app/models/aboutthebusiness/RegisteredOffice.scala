package models.aboutthebusiness

import models.Country
import models.FormTypes._
import models.businesscustomer.Address
import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json.{Writes, Reads, Json}

sealed trait RegisteredOffice {

  def toLines: Seq[String] = this match {
    case a: RegisteredOfficeUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: RegisteredOfficeNonUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.country.toString)
      ).flatten
  }
}

case class RegisteredOfficeUK(
                               addressLine1: String,
                               addressLine2: String,
                               addressLine3: Option[String] = None,
                               addressLine4: Option[String] = None,
                               postCode: String
                             ) extends RegisteredOffice

case class RegisteredOfficeNonUK(
                                  addressLine1: String,
                                  addressLine2: String,
                                  addressLine3: Option[String] = None,
                                  addressLine4: Option[String] = None,
                                  country: Country
                                ) extends RegisteredOffice

object RegisteredOffice {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, RegisteredOffice] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isUK").read[Boolean].withMessage("error.required.atb.registered.office.uk.or.overseas") flatMap {
      case true =>
        (
          (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) and
            (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) and
            (__ \ "addressLine3").read(optionR(validateAddress)) and
            (__ \ "addressLine4").read(optionR(validateAddress)) and
            (__ \ "postCode").read(postcodeType)
          ) (RegisteredOfficeUK.apply _)
      case false =>
        (
          (__ \ "addressLineNonUK1").read(notEmpty.withMessage("error.required.address.line1") compose  validateAddress) and
            (__ \ "addressLineNonUK2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) and
            (__ \ "addressLineNonUK3").read(optionR(validateAddress)) and
            (__ \ "addressLineNonUK4").read(optionR(validateAddress)) and
            (__ \ "country").read[Country]
          )(RegisteredOfficeNonUK.apply _)
    }
  }

  implicit val formWrites: Write[RegisteredOffice, UrlFormEncoded] = Write {
    case f: RegisteredOfficeUK =>
      Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> f.addressLine1,
        "addressLine2" -> f.addressLine2,
        "addressLine3" -> Seq(f.addressLine3.getOrElse("")),
        "addressLine4" -> Seq(f.addressLine4.getOrElse("")),
        "postCode" -> f.postCode
      )
    case f: RegisteredOfficeNonUK =>
      Map(
        "isUK" -> Seq("false"),
        "addressLineNonUK1" -> f.addressLine1,
        "addressLineNonUK2" -> f.addressLine2,
        "addressLineNonUK3" -> Seq(f.addressLine3.getOrElse("")),
        "addressLineNonUK4" -> Seq(f.addressLine4.getOrElse("")),
        "country" -> f.country.code
      )
  }

  implicit val jsonReads: Reads[RegisteredOffice] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
    (
      (__ \ "postCode").read[String] andKeep
        (
            (__ \ "addressLine1").read[String] and
            (__ \ "addressLine2").read[String] and
            (__ \ "addressLine3").readNullable[String] and
            (__ \ "addressLine4").readNullable[String] and
            (__ \ "postCode").read[String]
          ) (RegisteredOfficeUK.apply _) map identity[RegisteredOffice]
      ) orElse
      (
          (__ \ "addressLineNonUK1").read[String] and
          (__ \ "addressLineNonUK2").read[String] and
          (__ \ "addressLineNonUK3").readNullable[String] and
          (__ \ "addressLineNonUK4").readNullable[String] and
          (__ \ "country").read[Country]
        ) (RegisteredOfficeNonUK.apply _)
  }

  implicit val jsonWrites = Writes[RegisteredOffice] {
    case m: RegisteredOfficeUK =>
      Json.obj(
        "addressLine1" -> m.addressLine1,
        "addressLine2" -> m.addressLine2,
        "addressLine3" -> m.addressLine3,
        "addressLine4" -> m.addressLine4,
        "postCode" -> m.postCode)
    case m: RegisteredOfficeNonUK =>
      Json.obj(
        "addressLineNonUK1" -> m.addressLine1,
        "addressLineNonUK2" -> m.addressLine2,
        "addressLineNonUK3" -> m.addressLine3,
        "addressLineNonUK4" -> m.addressLine4,
        "country" -> m.country.code)
  }

  implicit def convert(address: Address): RegisteredOffice = {
    address.postcode match {
      case Some(data) => RegisteredOfficeUK (address.line_1,
        address.line_2,
        address.line_3,
        address.line_4,
        data)
      case None => RegisteredOfficeNonUK (address.line_1,
        address.line_2,
        address.line_3,
        address.line_4,
        address.country)
    }
  }
}