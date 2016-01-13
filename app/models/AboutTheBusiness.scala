package models

import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.libs.json._

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

case class RegisteredWithHMRCBefore(registeredWithHMRC: Boolean, mlrNumber: Option[String])

object RegisteredWithHMRCBefore {
  implicit val formats = Json.format[RegisteredWithHMRCBefore]
}
case class ContactingYouDetails(phoneNumber: String, email: String, website: String, sendLettersToThisAddress:Boolean)

object ContactingYouDetails {
  import play.api.libs.functional.syntax._

  implicit val formats = Json.format[ContactingYouDetails]
  implicit val formRule: Rule[UrlFormEncoded, ContactingYouDetails] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "phoneNumber").read(minLength(1)) and
        (__ \ "email").read(minLength(1)) and
        (__ \ "website").read[String] and
        (__ \ "sendLettersToThisAddress").read[Boolean]
      )(ContactingYouDetails.apply _)
  }

  implicit val formWrites: Write[ContactingYouDetails, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "phoneNumber").write[String] and
        (__ \ "email").write[String] and
        (__ \ "website").write[String] and
         (__ \ "sendLettersToThisAddress").write[Boolean]
         )(unlift(ContactingYouDetails.unapply _))
  }
}


case class ContactingYou(phoneNumber: String, email: String, website: String)

object ContactingYou {
  implicit val formats = Json.format[ContactingYou]

  implicit val formRule: Rule[UrlFormEncoded, ContactingYou] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "phoneNumber").read(minLength(1)) and
        (__ \ "email").read(minLength(1)) and
        (__ \ "website").read[String]
      )(ContactingYou.apply _)
  }

  implicit val formWrites: Write[ContactingYou, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "phoneNumber").write[String] and
        (__ \ "email").write[String] and
        (__ \ "website").write[String]
      )(unlift(ContactingYou.unapply _))
  }
}

/*sealed trait CorrespondenceAddress

object CorrespondenceAddress {

  implicit val formRule: Rule[UrlFormEncoded, CorrespondenceAddress] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "radioButton").read[Boolean].flatMap[CorrespondenceAddress] {
      case true =>
        (
          (__ \ "address_line1").read(minLength(1)) and
          (__ \ "address_line2").read(minLength(1)) and
          (__ \ "address_line3").read[Option[String]] and
          (__ \ "address_line4").read[Option[String]] and
          (__ \ "postcode").read[String]
        )(UKCorrespondenceAddress.apply _)
      case false =>
        (
          (__ \ "address_line1").read(minLength(1)) and
          (__ \ "address_line2").read(minLength(1)) and
          (__ \ "address_line3").read[Option[String]] and
          (__ \ "address_line4").read[Option[String]] and
          (__ \ "country").read[String]
        )(NonUKCorrespondenceAddress.apply _)
    }
  }


  implicit val jsonReads: Reads[CorrespondenceAddress] =
    (
      ((__ \ "postcode").read[String] andThen JsPath.read[UkCorrespondenceAddress]) orElse JsPath.read[NonUKCorrespondenceAddress]
      )

}*/

/*case class UKCorrespondenceAddress(address_line1: String,
                                   address_line2: String,
                                   address_line3: Option[String],
                                   address_line4: Option[String],
                                   postCode: String) extends CorrespondenceAddress

case class NonUKCorrespondenceAddress(address_line1: String,
                                      address_line2: String,
                                      address_line3: Option[String],
                                      address_line4: Option[String],
                                      country: String) extends CorrespondenceAddress*/

/*object UKCorrespondenceAddress {
  implicit val formats = Json.format[UKCorrespondenceAddress]

  implicit val formRule: Rule[UrlFormEncoded, UKCorrespondenceAddress] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "address_line1").read(minLength(1)) and
        (__ \ "address_line2").read(minLength(1)) and
        (__ \ "address_line3").read[Option[String]] and
        (__ \ "address_line4").read[Option[String]] and
        (__ \ "postcode").read[String]
      )(UKCorrespondenceAddress.apply _)
  }

  implicit val formWrites: Write[UKCorrespondenceAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "address_line1").write[String] and
      (__ \ "address_line2").write[String] and
      (__ \ "address_line3").write[Option[String]] and
      (__ \ "address_line4").write[Option[String]] and
        (__ \ "postcode").write[String]
      )(unlift(UKCorrespondenceAddress.unapply _))
  }

}*/

/*object NonUKCorrespondenceAddress {
  implicit val formats = Json.format[NonUKCorrespondenceAddress]

  implicit val formRule: Rule[UrlFormEncoded, NonUKCorrespondenceAddress] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "address_line1").read(minLength(1)) and
        (__ \ "address_line2").read(minLength(1)) and
        (__ \ "address_line3").read[Option[String]] and
        (__ \ "address_line4").read[Option[String]] and
        (__ \ "country").read[String]
      )(NonUKCorrespondenceAddress.apply _)
  }

  implicit val formWrites: Write[NonUKCorrespondenceAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "address_line1").write[String] and
      (__ \ "address_line2").write[String] and
      (__ \ "address_line3").write[Option[String]] and
      (__ \ "address_line4").write[Option[String]] and
        (__ \ "country").write[String]
      )(unlift(NonUKCorrespondenceAddress.unapply _))
  }

}*/

case class AboutTheBusiness(contactingYou: Option[ContactingYou] = None) {
  /*
    def registeredWithHMRCBefore(obj:RegisteredWithHMRCBefore): AboutTheBusiness = {
      this.copy(registeredWithHMRCBefore = Some(obj))
    }

    def businessWithVAT(obj: BusinessWithVAT) : AboutTheBusiness= {
      this.copy(businessWithVAT = Some(obj))
    }

    def confirmingYourAddress(obj:ConfirmingYourAddress) : AboutTheBusiness= {
      this.copy(confirmingYourAddress = Some(obj))
    }
  */

  def contactingYou(obj: ContactingYou):
  AboutTheBusiness = {
    this.copy(contactingYou = Some(obj))
  }

  /*def correspondenceAddress(obj: CorrespondenceAddress): AboutTheBusiness = {
    this.copy(correspondenceAddress = obj)
  }*/

}


object AboutTheBusiness {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val key = "about-the-business"
  implicit val formats = Json.format[AboutTheBusiness]

/*  implicit val reads: Reads[AboutTheBusiness] = (
        __.read[Option[ContactingYou]] and
          __.read[CorrespondenceAddress]
        ) (AboutTheBusiness.apply _)*/

/*  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
        model =>
          Seq(
            Json.toJson(model.contactingYou).asOpt[JsObject],
            Json.toJson(model.correspondenceAddress).asOpt[JsObject]
          ).flatten.fold(Json.obj()) {
            _ ++ _
          }
      }*/

  implicit def default(aboutTheBusiness: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutTheBusiness.getOrElse(AboutTheBusiness())

}