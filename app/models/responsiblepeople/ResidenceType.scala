package models.responsiblepeople

import models.FormTypes._
import org.joda.time.LocalDate
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.{Writes, Reads}


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
          (__ \ "dateOfBirth").read(localDateRule) and
            __.read[PassportType]
          )(NonUKResidence.apply _)
    }
  }

  implicit val formWrites: Write[ResidenceType, UrlFormEncoded] = Write {
    case f: UKResidence =>
      Map(
        "residenceType" -> Seq("true"),
        "nino" -> Seq(f.nino)
      )
    case f: NonUKResidence =>
      Map(
        "residenceType" -> Seq("false"),
        "dateOfBirth" -> f.dateOfBirth,
        "" ->(f.passportType)
      )
  }

  implicit val jsonReads: Reads[ResidenceType] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
      (__ \ "nino").read[String] andKeep (
            (__ \ "nino").read[String] fmap UKResidence.apply map identity[ResidenceType]
      ) orElse
      (
        (__ \ "dateOfBirth").read[LocalDate] and
          __.read[PassportType]
        ) (NonUKResidence.apply _)
  }

  implicit val jsonWrites: Writes[ResidenceType] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._

    Writes[ResidenceType] {
      case a: UKResidence =>
        (
          (__ \ "nino").write[String]
          )(unlift(UKResidence.unapply)).writes(a)
      case a: NonUKResidence =>
        (
          (__ \ "yourName").write[LocalDate] and
            (__ \ "businessName").write[PassportType]
          )(unlift(NonUKResidence.unapply)).writes(a)
    }
  }

}
