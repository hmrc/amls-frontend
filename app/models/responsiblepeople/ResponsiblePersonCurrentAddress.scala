package models.responsiblepeople

import models.DateOfChange
import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}


case class ResponsiblePersonCurrentAddress(personAddress: PersonAddress,
                                           timeAtAddress: TimeAtAddress,
                                           dateOfChange: Option[DateOfChange] = None)

object ResponsiblePersonCurrentAddress {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonCurrentAddress] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._
    (
      __.read[PersonAddress] ~
        __.read[TimeAtAddress]
      ) ((personAddress:PersonAddress, timeAtAddress: TimeAtAddress) => ResponsiblePersonCurrentAddress(personAddress, timeAtAddress, None))
  }

  def unapplyNoDateOfChange(currentAddress:ResponsiblePersonCurrentAddress):Option[(PersonAddress,TimeAtAddress)] =
    Some((currentAddress.personAddress,currentAddress.timeAtAddress))

  implicit val formWrites: Write[ResponsiblePersonCurrentAddress, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      __.write[PersonAddress] ~
        __.write[TimeAtAddress]
      ) (unlift(ResponsiblePersonCurrentAddress.unapplyNoDateOfChange))
  }

  implicit val format = Json.format[ResponsiblePersonCurrentAddress]

}
