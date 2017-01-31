package models.responsiblepeople

import jto.validation.{To, Write, From, Rule}
import jto.validation.forms._
import play.api.libs.json.Json
import models.FormTypes._

case class ContactDetails(phoneNumber: String, emailAddress: String)

object ContactDetails {

  import utils.MappingUtils.Implicits._

  implicit val formats = Json.format[ContactDetails]

  implicit val formReads: Rule[UrlFormEncoded, ContactDetails] = From[UrlFormEncoded] { __ =>
    import models.FormTypes._
    import jto.validation.forms.Rules._
    (
      (__ \ "phoneNumber").read(phoneNumberType) ~
        (__ \ "emailAddress").read(emailType)
    )(ContactDetails.apply _)
  }

  implicit val formWrites: Write[ContactDetails, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import scala.Function.unlift
    (
      (__ \ "phoneNumber").write[String] ~
        (__ \ "emailAddress").write[String]
      ) (unlift(ContactDetails.unapply _))
  }

}
