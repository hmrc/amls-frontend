package models.responsiblepeople

import jto.validation.{To, Write, From, Rule}
import jto.validation.forms._


case class ResponsiblePersonAddress(personAddress: PersonAddress,
                                    timeAtAddress: Option[TimeAtAddress])

object ResponsiblePersonAddress {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonAddress] = From[UrlFormEncoded] { __ =>

  import jto.validation.forms.Rules._
    (__.read[PersonAddress] ~ (__ \ "timeAtAddress").read[Option[TimeAtAddress]]) (ResponsiblePersonAddress.apply _)
}

  implicit val formWrites: Write[ResponsiblePersonAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
        __.write[PersonAddress] ~
          __.write[Option[TimeAtAddress]]
      ) (unlift(ResponsiblePersonAddress.unapply))
  }

  implicit val format = Json.format[ResponsiblePersonAddress]

}

