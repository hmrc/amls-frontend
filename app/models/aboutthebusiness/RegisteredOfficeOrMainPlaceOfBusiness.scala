package models.aboutthebusiness

import models.FormTypes._
import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.libs.json.{Writes, Reads, Json}

sealed trait RegisteredOfficeOrMainPlaceOfBusiness

case class RegisteredOfficeOrMainPlaceOfBusinessUK(
                                             addressLine1: String,
                                             addressLine2: String,
                                             addressLine3: Option[String] = None,
                                             addressLine4: Option[String] = None,
                                             postCode : String
                                           ) extends RegisteredOfficeOrMainPlaceOfBusiness

case class RegisteredOfficeOrMainPlaceOfBusinessNonUK(
                                                addressLine1: String,
                                                addressLine2: String,
                                                addressLine3: Option[String] = None,
                                                addressLine4: Option[String] = None,
                                                country : String
                                              ) extends RegisteredOfficeOrMainPlaceOfBusiness

object RegisteredOfficeOrMainPlaceOfBusiness {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, RegisteredOfficeOrMainPlaceOfBusiness] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isUKOrOverseas").read[Boolean] flatMap[RegisteredOfficeOrMainPlaceOfBusiness] {
      case true =>
        (
          (__ \ "addressLine1").read(addressType) and
            (__ \ "addressLine2").read(addressType) and
            (__ \ "addressLine3").read[Option[String]] and
            (__ \ "addressLine4").read[Option[String]] and
            (__ \ "postCode").read(postCodeType)
          )(RegisteredOfficeOrMainPlaceOfBusinessUK.apply _)
      case false =>
        (
          (__ \ "addressLineNonUK1").read(addressType) and
            (__ \ "addressLineNonUK2").read(addressType) and
            (__ \ "addressLineNonUK3").read[Option[String]] and
            (__ \ "addressLineNonUK4").read[Option[String]] and
            (__ \ "country").read(countryType)
          )(RegisteredOfficeOrMainPlaceOfBusinessNonUK.apply _)
    }
  }

  implicit val formWrites: Write[RegisteredOfficeOrMainPlaceOfBusiness, UrlFormEncoded] = Write {
    case f: RegisteredOfficeOrMainPlaceOfBusinessUK =>
      Map(
        "isUKOrOverseas" -> Seq("true"),
        "addressLine1" -> f.addressLine1,
        "addressLine2" -> f.addressLine2,
        "addressLine3" -> Seq(f.addressLine3.getOrElse("")),
        "addressLine4" -> Seq(f.addressLine4.getOrElse("")),
        "postCode" -> f.postCode
      )
    case f: RegisteredOfficeOrMainPlaceOfBusinessNonUK =>
      Map(
        "isUKOrOverseas" -> Seq("false"),
        "addressLineNonUK1" -> f.addressLine1,
        "addressLineNonUK2" -> f.addressLine2,
        "addressLineNonUK3" -> Seq(f.addressLine3.getOrElse("")),
        "addressLineNonUK4" -> Seq(f.addressLine4.getOrElse("")),
        "country" -> f.country
      )
  }

  implicit val jsonReads: Reads[RegisteredOfficeOrMainPlaceOfBusiness] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
    (
      (__ \ "postCode").read[String] andKeep
        (
          (__ \ "addressLine1").read[String] and
            (__ \ "addressLine2").read[String] and
            (__ \ "addressLine3").read[Option[String]] and
            (__ \ "addressLine4").read[Option[String]] and
            (__ \ "postCode").read[String]
          )(RegisteredOfficeOrMainPlaceOfBusinessUK.apply _) map identity[RegisteredOfficeOrMainPlaceOfBusiness]
      ) orElse
      (
        (__ \ "addressLineNonUK1").read[String] and
          (__ \ "addressLineNonUK2").read[String] and
          (__ \ "addressLineNonUK3").read[Option[String]] and
          (__ \ "addressLineNonUK4").read[Option[String]] and
          (__ \ "country").read[String]
        )(RegisteredOfficeOrMainPlaceOfBusinessNonUK.apply _)
  }

  implicit val jsonWrites = Writes[RegisteredOfficeOrMainPlaceOfBusiness] {
    case m: RegisteredOfficeOrMainPlaceOfBusinessUK =>
      Json.obj("isUKOrOverseas" -> true,
        "addressLine1" -> m.addressLine1,
        "addressLine2" -> m.addressLine2,
        "addressLine3" -> m.addressLine3,
        "addressLine4" -> m.addressLine4,
        "postCode" -> m.postCode)
    case m: RegisteredOfficeOrMainPlaceOfBusinessNonUK =>
      Json.obj( "isUKOrOverseas" -> false,
        "addressLineNonUK1" -> m.addressLine1,
        "addressLineNonUK2" -> m.addressLine2,
        "addressLineNonUK3" -> m.addressLine3,
        "addressLineNonUK4" -> m.addressLine4,
        "country" -> m.country)
  }
}