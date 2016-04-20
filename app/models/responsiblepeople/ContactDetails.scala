package models.responsiblepeople

import play.api.data.mapping.{To, Write, From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json.Json
import models.FormTypes._

case class ContactDetails(phoneNumber: String, emailAddress: String)

object ContactDetails {

  import utils.MappingUtils.Implicits._

  implicit val formats = Json.format[ContactDetails]

  implicit val formReads: Rule[UrlFormEncoded, ContactDetails] = From[UrlFormEncoded] { __ =>
    import models.FormTypes._
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "phoneNumber").read(phoneNumberType) and
        (__ \ "emailAddress").read(emailType)
    )(ContactDetails.apply _)
  }

  implicit val formWrites: Write[ContactDetails, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "phoneNumber").write[String] and
        (__ \ "emailAddress").write[String]
      ) (unlift(ContactDetails.unapply _))
  }

}
