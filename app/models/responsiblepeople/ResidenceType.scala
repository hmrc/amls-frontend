package models.responsiblepeople

import models.FormTypes._
import org.joda.time.LocalDate
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.{Writes, Reads}

sealed trait ResidenceType

case class UKResidence(nino: String) extends ResidenceType

case class NonUKResidence(
                           dateOfBirth: LocalDate,
                           passportType: PassportType
                         ) extends ResidenceType

object ResidenceType {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ResidenceType] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "isUKResidence").read[Boolean].withMessage("error.required.rp.is.uk.resident") flatMap {
      case true =>
        (__ \ "nino").read(ninoType).map(UKResidence.apply)
      case false =>
        (
          (__ \ "dateOfBirth").read(localDateRule) ~
            __.read[PassportType]
          ) (NonUKResidence)
    }
  }

  implicit def formWrites
  (implicit
   w: Write[NonUKResidence, UrlFormEncoded]
  ) = Write[ResidenceType, UrlFormEncoded] {
    case f: UKResidence =>
      Map(
        "isUKResidence" -> Seq("true"),
        "nino" -> Seq(f.nino)
      )
    case f: NonUKResidence =>
      Map(
        "isUKResidence" -> Seq("false")
      ) ++
      w.writes(f)

  }

  implicit val formWritesNonUK: Write[NonUKResidence, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import models.FormTypes.localDateWrite
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "dateOfBirth").write(localDateWrite) ~
        __.write[PassportType]
      ) (unlift(NonUKResidence.unapply))
  }


  implicit val jsonReads: Reads[ResidenceType] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
      (__ \ "nino").read[String] map UKResidence.apply map identity[ResidenceType] orElse
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
        Json.obj(
          "nino" -> a.nino
        )
      case a: NonUKResidence =>
        (
          (__ \ "dateOfBirth").write[LocalDate] and
            __.write[PassportType]
          ) (unlift(NonUKResidence.unapply)).writes(a)
    }
  }

}
