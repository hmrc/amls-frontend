package models.aboutthebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
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
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "phoneNumber").write[String] and
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

  implicit val formRule: Rule[UrlFormEncoded, ContactingYouForm] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "phoneNumber").read(phoneNumberType) and
          (__ \ "email").read(emailType) and
          (__ \ "letterToThisAddress").read[Boolean]
        )(ContactingYouForm.apply _)
    }

}
