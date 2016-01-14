package models

import play.api.libs.json._
import play.api.libs.json.Json

import scala.language.implicitConversions
/*
case class BCAddress(
                      line_1: String,
                      line_2: String,
                      line_3: Option[String] = None,
                      line_4: Option[String] = None,
                      postcode: Option[String] = None,
                      country: String) {

  def render = {
    val line3display = line_3.map(line3 => s"$line3, ").getOrElse("")
    val line4display = line_4.map(line4 => s"$line4, ").getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }
}

object BCAddress {
  implicit val formats = Json.format[BCAddress]
}
*/

case class BusinessWithVAT(hasVAT: Boolean, VATNum: Option[String])

object BusinessWithVAT {
  implicit val formats = Json.format[BusinessWithVAT]
}

case class ContactingYou(phoneNumber: String, email: String, website: Option[String], letterToThisAddress: Boolean)

object ContactingYou {
  implicit val formats = Json.format[ContactingYou]
}

case class RegisteredWithHMRCBefore(registeredWithHMRC: Boolean, mlrNumber: Option[String])

object RegisteredWithHMRCBefore {
  implicit val formats = Json.format[RegisteredWithHMRCBefore]
}

case class AboutTheBusiness(
                             registeredWithHMRCBefore: Option[RegisteredWithHMRCBefore] = None,
                             businessWithVAT: Option[BusinessWithVAT] = None,
                             contactingYou: Option[ContactingYou] = None
                           ) {
  def registeredWithHMRCBefore(obj:RegisteredWithHMRCBefore): AboutTheBusiness = {
    this.copy(registeredWithHMRCBefore = Some(obj))
  }

  def businessWithVAT(obj: BusinessWithVAT) : AboutTheBusiness= {
    this.copy(businessWithVAT = Some(obj))
  }
  def contactingYou(obj:ContactingYou):AboutTheBusiness = {
    this.copy(contactingYou= Some(obj))
  }
}

object AboutTheBusiness {
  val key = "about-the-business"

/*  implicit val reads: Reads[AboutTheBusiness] = (
      __.read[Option[RegisteredWithHMRCBefore]] and
      __.read[Option[BusinessWithVAT]] and
      __.read[Option[ConfirmingYourAddress]] and
      __.read[Option[ContactingYou]]
    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.registeredWithHMRCBefore).asOpt[JsObject],
        Json.toJson(model.businessWithVAT).asOpt[JsObject],
        Json.toJson(model.confirmingYourAddress).asOpt[JsObject],
        Json.toJson(model.contactingYou).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }*/
  implicit def default(aboutTheBusiness: Option[AboutTheBusiness]): AboutTheBusiness =
      aboutTheBusiness.getOrElse(AboutTheBusiness())

}