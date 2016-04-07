package models.responsiblepeople

import play.api.data.mapping.{To, Write, From, Rule}
import play.api.data.mapping.forms._

case class PersonAddressHistory(personAddress: PersonAddress,
                                personHistory: PersonHistory)

object PersonAddressHistory {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, PersonAddressHistory] = From[UrlFormEncoded] { __ =>

  import play.api.data.mapping.forms.Rules._
  (
    (__).read[PersonAddress] and
      (__).read[PersonHistory]
    ) (PersonAddressHistory.apply _)
}

  implicit val formWrites: Write[PersonAddressHistory, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
        (__).write[PersonAddress] and
          (__).write[PersonHistory]
      ) (unlift(PersonAddressHistory.unapply))
  }

  implicit val format = Json.format[PersonAddressHistory]

}

