package models.responsiblepeople

import jto.validation.{From, Rule, To, Write}
import jto.validation.forms.UrlFormEncoded

case class NewHomeAddress(personAddress: PersonAddress)

object NewHomeAddress {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, NewHomeAddress] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._
      __.read[PersonAddress] map NewHomeAddress.apply _

  }

  implicit val formWrites: Write[NewHomeAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    __.write[PersonAddress] contramap{x =>x.personAddress}
  }

  implicit val format = Json.format[NewHomeAddress]

}