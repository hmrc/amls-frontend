package models.aboutthebusiness

import models.FormTypes._
import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.libs.json.{Writes, Reads, Json}

sealed trait WhereIsRegOfficeOrMainPlaceOfBusiness

case class RegOfficeOrMainPlaceOfBusinessUK(
                                             addressLine1: String,
                                             addressLine2: String,
                                             addressLine3: Option[String],
                                             addressLine4: Option[String],
                                             postCode : String
                                           ) extends WhereIsRegOfficeOrMainPlaceOfBusiness

case class RegOfficeOrMainPlaceOfBusinessNonUK(
                                                addressLine1: String,
                                                addressLine2: String,
                                                addressLine3: Option[String],
                                                addressLine4: Option[String],
                                                country : String
                                              ) extends WhereIsRegOfficeOrMainPlaceOfBusiness

object WhereIsRegOfficeOrMainPlaceOfBusiness {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, WhereIsRegOfficeOrMainPlaceOfBusiness] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isUKOrOverseas").read[Boolean] flatMap[WhereIsRegOfficeOrMainPlaceOfBusiness] {
      case true =>
        (
          (__ \ "addressLine1").read(addressType) and
            (__ \ "addressLine2").read(addressType) and
            (__ \ "addressLine3").read[Option[String]] and
            (__ \ "addressLine4").read[Option[String]] and
            (__ \ "postCode").read(postCodeType)
          )(RegOfficeOrMainPlaceOfBusinessUK.apply _)
      case false =>
        (
          (__ \ "addressLine1").read(addressType) and
            (__ \ "addressLine2").read(addressType) and
            (__ \ "addressLine3").read[Option[String]] and
            (__ \ "addressLine4").read[Option[String]] and
            (__ \ "country").read(countryType)
          )(RegOfficeOrMainPlaceOfBusinessNonUK.apply _)
    }
  }

  implicit val formWrites: Write[WhereIsRegOfficeOrMainPlaceOfBusiness, UrlFormEncoded] = Write {
    case f: RegOfficeOrMainPlaceOfBusinessUK =>
      Map(
        "isUKOrOverseas" -> Seq("true"),
        "addressLine1" -> f.addressLine1,
        "addressLine2" -> f.addressLine1,
        "addressLine3" -> f.addressLine1,
        "addressLine4" -> f.addressLine1,
        "postCode" -> f.postCode
      )
    case f: RegOfficeOrMainPlaceOfBusinessNonUK =>
      Map(
        "isUKOrOverseas" -> Seq("false"),
        "addressLine1" -> f.addressLine1,
        "addressLine2" -> f.addressLine1,
        "addressLine3" -> f.addressLine1,
        "addressLine4" -> f.addressLine1,
        "country" -> f.country
      )
  }

  implicit val jsonReads: Reads[WhereIsRegOfficeOrMainPlaceOfBusiness] = {
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
          )(RegOfficeOrMainPlaceOfBusinessUK.apply _) map identity[WhereIsRegOfficeOrMainPlaceOfBusiness]
      ) orElse
      (
        (__ \ "addressLine1").read[String] and
          (__ \ "addressLine2").read[String] and
          (__ \ "addressLine3").read[Option[String]] and
          (__ \ "addressLine4").read[Option[String]] and
          (__ \ "country").read[String]
        )(RegOfficeOrMainPlaceOfBusinessNonUK.apply _)
  }

  implicit val jsonWrites = Writes[WhereIsRegOfficeOrMainPlaceOfBusiness] {
    case m: RegOfficeOrMainPlaceOfBusinessUK =>
      Json.obj("isUKOrOverseas" -> true,
        "addressLine1" -> m.addressLine1,
        "addressLine2" -> m.addressLine2,
        "addressLine3" -> m.addressLine3,
        "addressLine4" -> m.addressLine4,
        "postCode" -> m.postCode)
    case m: RegOfficeOrMainPlaceOfBusinessNonUK =>
      Json.obj( "isUKOrOverseas" -> false,
        "addressLine1" -> m.addressLine1,
        "addressLine2" -> m.addressLine2,
        "addressLine3" -> m.addressLine3,
        "addressLine4" -> m.addressLine4,
        "country" -> m.country)
  }
}