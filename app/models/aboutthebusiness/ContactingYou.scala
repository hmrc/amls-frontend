package models.aboutthebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.Json

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

case class ContactingYouDetails(phoneNumber: String, email: String, website: String, sendLettersToThisAddress: Boolean)

object ContactingYouDetails {

  implicit val formats = Json.format[ContactingYouDetails]
  implicit val formRule: Rule[UrlFormEncoded, ContactingYouDetails] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "phoneNumber").read(minLength(1)) and
        (__ \ "email").read(minLength(1)) and
        (__ \ "website").read[String] and
        (__ \ "letterToThisAddress").read[Boolean]
      )(ContactingYouDetails.apply _)
  }

  implicit val formWrites: Write[ContactingYouDetails, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "phoneNumber").write[String] and
        (__ \ "email").write[String] and
        (__ \ "website").write[String] and
        (__ \ "letterToThisAddress").write[Boolean]
      )(unlift(ContactingYouDetails.unapply _))
  }
}
