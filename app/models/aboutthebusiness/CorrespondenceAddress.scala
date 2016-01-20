package models.aboutthebusiness

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.{Reads, Writes}

sealed trait CorrespondenceAddress {

  def toLines: Seq[String] = this match {
    case a: UKCorrespondenceAddress =>
      Seq(
        Some(a.yourName),
        Some(a.businessName),
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: NonUKCorrespondenceAddress =>
      Seq(
        Some(a.yourName),
        Some(a.businessName),
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.country)
      ).flatten
  }
}

case class UKCorrespondenceAddress(
                                  yourName: String,
                                  businessName: String,
                                  addressLine1: String,
                                  addressLine2: String,
                                  addressLine3: Option[String],
                                  addressLine4: Option[String],
                                  postCode: String
                                  ) extends CorrespondenceAddress

case class NonUKCorrespondenceAddress(
                                     yourName: String,
                                     businessName: String,
                                     addressLine1: String,
                                     addressLine2: String,
                                     addressLine3: Option[String],
                                     addressLine4: Option[String],
                                     country: String
                                     ) extends CorrespondenceAddress

object CorrespondenceAddress {

  implicit val formRule: Rule[UrlFormEncoded, CorrespondenceAddress] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      import models.FormTypes._

      val nameType = notEmpty compose maxLength(140)

      (__ \ "isUK").read[Boolean] flatMap {
        case true => (
            (__ \ "yourName").read(nameType) ~
            (__ \ "businessName").read(nameType) ~
            (__ \ "addressLine1").read(addressType) ~
            (__ \ "addressLine2").read(addressType) ~
            (__ \ "addressLine3").read(optionR(addressType)) ~
            (__ \ "addressLine4").read(optionR(addressType)) ~
            (__ \ "postCode").read(postCodeType)
          )(UKCorrespondenceAddress.apply _)
        case false => (
            (__ \ "yourName").read(nameType) ~
            (__ \ "businessName").read(nameType) ~
            (__ \ "addressLine1").read(addressType) ~
            (__ \ "addressLine2").read(addressType) ~
            (__ \ "addressLine3").read(optionR(addressType)) ~
            (__ \ "addressLine4").read(optionR(addressType)) ~
            (__ \ "country").read(countryType)
          )(NonUKCorrespondenceAddress.apply _)
      }
    }

  implicit val formWrites = Write[CorrespondenceAddress, UrlFormEncoded] {
    case a: UKCorrespondenceAddress =>
      Map(
        "isUK" -> Seq("true"),
        "yourName" -> Seq(a.yourName),
        "businessName" -> Seq(a.businessName),
        "addressLine1" -> Seq(a.addressLine1),
        "addressLine2" -> Seq(a.addressLine2),
        "addressLine3" -> a.addressLine3.toSeq,
        "addressLine4" -> a.addressLine4.toSeq,
        "postCode" -> Seq(a.postCode)
      )
    case a: NonUKCorrespondenceAddress =>
      Map(
        "isUK" -> Seq("false"),
        "yourName" -> Seq(a.yourName),
        "businessName" -> Seq(a.businessName),
        "addressLine1" -> Seq(a.addressLine1),
        "addressLine2" -> Seq(a.addressLine2),
        "addressLine3" -> a.addressLine3.toSeq,
        "addressLine4" -> a.addressLine4.toSeq,
        "country" -> Seq(a.country)
      )
  }

  implicit val jsonReads: Reads[CorrespondenceAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "isUK").read[Boolean] flatMap {
      case true => (
          (__ \ "yourName").read[String] and
          (__ \ "businessName").read[String] and
          (__ \ "correspondenceAddressLine1").read[String] and
          (__ \ "correspondenceAddressLine2").read[String] and
          (__ \ "correspondenceAddressLine3").read[Option[String]] and
          (__ \ "correspondenceAddressLine4").read[Option[String]] and
          (__ \ "correspondencePostCode").read[String]
        )(UKCorrespondenceAddress.apply _)
      case false => (
          (__ \ "yourName").read[String] and
          (__ \ "businessName").read[String] and
          (__ \ "correspondenceAddressLine1").read[String] and
          (__ \ "correspondenceAddressLine2").read[String] and
          (__ \ "correspondenceAddressLine3").read[Option[String]] and
          (__ \ "correspondenceAddressLine4").read[Option[String]] and
          (__ \ "correspondenceCountry").read[String]
        )(NonUKCorrespondenceAddress.apply _)
    }
  }

  implicit val jsonWrites: Writes[CorrespondenceAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[CorrespondenceAddress] {
      case a: UKCorrespondenceAddress =>
        (
          (__ \ "yourName").write[String] and
          (__ \ "businessName").write[String] and
          (__ \ "correspondenceAddressLine1").write[String] and
          (__ \ "correspondenceAddressLine2").write[String] and
          (__ \ "correspondenceAddressLine3").writeNullable[String] and
          (__ \ "correspondenceAddressLine4").writeNullable[String] and
          (__ \ "correspondencePostCode").write[String]
        )(unlift(UKCorrespondenceAddress.unapply)).writes(a)
      case a: NonUKCorrespondenceAddress =>
        (
          (__ \ "yourName").write[String] and
          (__ \ "businessName").write[String] and
          (__ \ "correspondenceAddressLine1").write[String] and
          (__ \ "correspondenceAddressLine2").write[String] and
          (__ \ "correspondenceAddressLine3").writeNullable[String] and
          (__ \ "correspondenceAddressLine4").writeNullable[String] and
          (__ \ "correspondencePostCode").write[String]
        )(unlift(NonUKCorrespondenceAddress.unapply)).writes(a)
    }
  }
}
