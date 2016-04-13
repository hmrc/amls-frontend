package models.responsiblepeople

import play.api.data.mapping.{To, Write, From, Rule}
import play.api.data.mapping.forms._


case class ResponsiblePersonAddress(personAddress: PersonAddress,
                                    timeAtAddress: TimeAtAddress)

object ResponsiblePersonAddress {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonAddress] = From[UrlFormEncoded] { __ =>

  import play.api.data.mapping.forms.Rules._
  (
    (__).read[PersonAddress] and
      (__).read[TimeAtAddress]
    ) (ResponsiblePersonAddress.apply _)
}

  implicit val formWrites: Write[ResponsiblePersonAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
        (__).write[PersonAddress] and
          (__).write[TimeAtAddress]
      ) (unlift(ResponsiblePersonAddress.unapply))
  }

  implicit val format = Json.format[ResponsiblePersonAddress]

}

