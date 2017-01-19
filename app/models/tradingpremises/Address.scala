package models.tradingpremises

import models.DateOfChange
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.{Json, Reads, Writes}

case class Address(
                  addressLine1: String,
                  addressLine2: String,
                  addressLine3: Option[String],
                  addressLine4: Option[String],
                  postcode: String,
                  dateOfChange: Option[DateOfChange] = None
                  ) {

  def toLines: Seq[String] = Seq(
    Some(addressLine1),
    Some(addressLine2),
    addressLine3,
    addressLine4,
    Some(postcode)
  ).flatten
}

object Address {
  import utils.MappingUtils.Implicits._

  def applyWithoutDateOfChange(address1: String, address2: String, address3: Option[String], address4: Option[String], postcode: String) =
    Address(address1, address2, address3, address4, postcode)

  def unapplyWithoutDateOfChange(x: Address) =
    Some((x.addressLine1, x.addressLine2, x.addressLine3, x.addressLine4, x.postcode))

  implicit val formR: Rule[UrlFormEncoded, Address] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "addressLine1").read(notEmptyStrip.withMessage("error.required.address.line1") compose validateAddress) ~
          (__ \ "addressLine2").read(notEmptyStrip.withMessage("error.required.address.line2") compose validateAddress) ~
          (__ \ "addressLine3").read(notEmptyStrip compose validateAddress compose valueOrNone) ~
          (__ \ "addressLine4").read(notEmptyStrip compose validateAddress compose valueOrNone) ~
          (__ \ "postcode").read(postcodeType)
        )(Address.applyWithoutDateOfChange _)
    }

  implicit val formW: Write[Address, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      (
        (__ \ "addressLine1").write[String] ~
          (__ \ "addressLine2").write[String] ~
          (__ \ "addressLine3").write[Option[String]] ~
          (__ \ "addressLine4").write[Option[String]] ~
          (__ \ "postcode").write[String]
        )(unlift(Address.unapplyWithoutDateOfChange))
    }
  
  implicit val reads: Reads[Address] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "addressLine1").read[String] and
        (__ \ "addressLine2").read[String] and
        (__ \ "addressLine3").readNullable[String] and
        (__ \ "addressLine4").readNullable[String] and
        (__ \ "postcode").read[String] and
        (__ \ "addressDateOfChange").readNullable[DateOfChange]
      )(Address.apply _)
  }
  
  implicit val writes: Writes[Address] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "addressLine1").write[String] and
        (__ \ "addressLine2").write[String] and
        (__ \ "addressLine3").writeNullable[String] and
        (__ \ "addressLine4").writeNullable[String] and
        (__ \ "postcode").write[String] and
        (__ \ "addressDateOfChange").writeNullable[DateOfChange]
      )(unlift(Address.unapply))
  }
}
