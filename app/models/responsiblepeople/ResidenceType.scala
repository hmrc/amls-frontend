/*
package models.responsiblepeople

import org.joda.time.LocalDate
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.{Reads, Writes}


sealed trait ResidenceType

case class UKResidence (nino: String) extends ResidenceType

case class NonUKResidence (
                          dateOfBirth: LocalDate,
                          passportType: PassportType
                          ) extends ResidenceType


object ResidenceType {

  implicit val formRule: Rule[UrlFormEncoded, ResidenceType] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "residenceType").read[Boolean] flatMap {
      case true =>
        (__ \ "nino").read[String].fmap(UKResidence.apply)
      case false =>
        (
          (__ \ "dateOfBirth").read[LocalDate] and
            __.read[PassportType]
          )(NonUKResidence.apply _)
    }
  }




}*/
