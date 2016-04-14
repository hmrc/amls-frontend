package models.aboutthebusiness

import models.Country
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{Path, From, Rule, Write}
import play.api.data.validation.ValidationError
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
        Some(a.addressLineNonUK1),
        Some(a.addressLineNonUK2),
        a.addressLineNonUK3,
        a.addressLineNonUK4,
        Some(a.country.toString)
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
                                     addressLineNonUK1: String,
                                     addressLineNonUK2: String,
                                     addressLineNonUK3: Option[String],
                                     addressLineNonUK4: Option[String],
                                     country: Country
                                     ) extends CorrespondenceAddress

object CorrespondenceAddress {
  implicit val formRule: Rule[UrlFormEncoded, CorrespondenceAddress] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      import models.FormTypes._
      import utils.MappingUtils.Implicits._
      val nameMaxLength = 140
      val nameType = maxLength(nameMaxLength).withMessage("error.invalid.yourname")
      val businessNameMaxLength = 120
      val businessNameType =  maxLength(businessNameMaxLength).withMessage("error.invalid.name.of.business")

      (__ \ "isUK").read[Boolean].withMessage("error.required.uk.or.overseas") flatMap {
        case true => (
            (__ \ "yourName").read(notEmpty.withMessage("error.required.yourname") compose nameType) ~
            (__ \ "businessName").read(notEmpty.withMessage("error.required.name.of.business") compose businessNameType) ~
            (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) ~
            (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) ~
            (__ \ "addressLine3").read(optionR(validateAddress)) ~
            (__ \ "addressLine4").read(optionR(validateAddress)) ~
            (__ \ "postCode").read(postcodeType)
          )(UKCorrespondenceAddress.apply _)
        case false => (
            (__ \ "yourName").read(notEmpty.withMessage("error.required.yourname") compose nameType) ~
            (__ \ "businessName").read(notEmpty.withMessage("error.required.name.of.business") compose businessNameType) ~
            (__ \ "addressLineNonUK1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) ~
            (__ \ "addressLineNonUK2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) ~
            (__ \ "addressLineNonUK3").read(optionR(validateAddress)) ~
            (__ \ "addressLineNonUK4").read(optionR(validateAddress)) ~
              (__ \ "country").read[Country]
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
        "addressLineNonUK1" -> Seq(a.addressLineNonUK1),
        "addressLineNonUK2" -> Seq(a.addressLineNonUK2),
        "addressLineNonUK3" -> a.addressLineNonUK3.toSeq,
        "addressLineNonUK4" -> a.addressLineNonUK4.toSeq,
        "country" -> Seq(a.country.code)
      )
  }

  implicit val jsonReads: Reads[CorrespondenceAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "correspondencePostCode").read[String] andKeep (
      ((__ \ "yourName").read[String] and
        (__ \ "businessName").read[String] and
        (__ \ "correspondenceAddressLine1").read[String] and
        (__ \ "correspondenceAddressLine2").read[String] and
        (__ \ "correspondenceAddressLine3").readNullable[String] and
        (__ \ "correspondenceAddressLine4").readNullable[String] and
        (__ \ "correspondencePostCode").read[String])(UKCorrespondenceAddress.apply _) map identity[CorrespondenceAddress]
      ) orElse
      ((__ \ "yourName").read[String] and
        (__ \ "businessName").read[String] and
        (__ \ "correspondenceAddressLine1").read[String] and
        (__ \ "correspondenceAddressLine2").read[String] and
        (__ \ "correspondenceAddressLine3").readNullable[String] and
        (__ \ "correspondenceAddressLine4").readNullable[String] and
        (__ \ "correspondenceCountry").read[Country])(NonUKCorrespondenceAddress.apply _)

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
          (__ \ "correspondenceCountry").write[Country]
        )(unlift(NonUKCorrespondenceAddress.unapply)).writes(a)
    }
  }
}

