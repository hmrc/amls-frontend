package models.tradingpremises

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.Json

case class Address(
                  addressLine1: String,
                  addressLine2: String,
                  addressLine3: Option[String],
                  addressLine4: Option[String],
                  postcode: String
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

  implicit val formR: Rule[UrlFormEncoded, Address] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "addressLine1").read(notEmpty.withMessage("error.required.address.line1") compose validateAddress) ~
          (__ \ "addressLine2").read(notEmpty.withMessage("error.required.address.line2") compose validateAddress) ~
          (__ \ "addressLine3").read(optionR(validateAddress)) ~
          (__ \ "addressLine4").read(optionR(validateAddress)) ~
          (__ \ "postcode").read(postcodeType)
        )(Address.apply _)
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
        )(unlift(Address.unapply _))
    }

  implicit val format = Json.format[Address]
}
