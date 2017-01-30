package models.aboutthebusiness

import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.Json

case class ContactingYou(
                          phoneNumber: String,
                          email: String
                        )

object ContactingYou {

  implicit def convert(c: ContactingYouForm): ContactingYou =
    ContactingYou(
      phoneNumber = c.phoneNumber,
      email = c.email
    )

  implicit val formats = Json.format[ContactingYou]

  implicit val formWrites: Write[ContactingYou, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import scala.Function.unlift
    (
      (__ \ "phoneNumber").write[String] ~
        (__ \ "email").write[String]
      ) (unlift(ContactingYou.unapply _))
  }
}

case class ContactingYouForm(
                              phoneNumber: String,
                              email: String,
                              letterToThisAddress: Boolean
                            )

object ContactingYouForm {

  implicit val formats = Json.format[ContactingYouForm]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ContactingYouForm] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import jto.validation.forms.Rules._
      (
        (__ \ "phoneNumber").read(phoneNumberType) ~
          (__ \ "email").read(emailType) ~
          (__ \ "letterToThisAddress").read[Boolean].withMessage("error.required.rightaddress")
        )(ContactingYouForm.apply _)
    }
}
