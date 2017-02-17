package models.responsiblepeople

import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.Json

case class ContactDetails(phoneNumber: String, emailAddress: String)

object ContactDetails {

  implicit val formats = Json.format[ContactDetails]

  implicit val formReads: Rule[UrlFormEncoded, ContactDetails] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (
      (__ \ "phoneNumber").read(phoneNumberType) ~
        (__ \ "emailAddress").read(emailType)
    )(ContactDetails.apply)
  }

  implicit val formWrites: Write[ContactDetails, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._

    import scala.Function.unlift
    (
      (__ \ "phoneNumber").write[String] ~
        (__ \ "emailAddress").write[String]
      ) (unlift(ContactDetails.unapply))
  }

}
