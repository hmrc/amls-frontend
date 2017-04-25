package models.responsiblepeople

import models.Country
import jto.validation._
import jto.validation.forms._
import play.api.libs.json.{Reads, Writes}

case class PersonResidenceType (
                               isUKResidence : ResidenceType,
                               countryOfBirth: Country,
                               nationality: Option[Country]
                              )

object PersonResidenceType {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PersonResidenceType] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (
      __.read[ResidenceType] ~
      (__ \ "countryOfBirth").read[Country].withMessage("error.required.rp.birth.country") ~
      (__ \ "nationality").read[Option[Country]].withMessage("error.required.nationality")
      )(PersonResidenceType.apply)
  }

  implicit val formWrites: Write[PersonResidenceType, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      __.write[ResidenceType] ~
        (__ \ "countryOfBirth").write[Country] ~
        (__ \ "nationality").write[Option[Country]]
      ) (unlift(PersonResidenceType.unapply))
  }

  implicit val jsonRead: Reads[PersonResidenceType] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      __.read[ResidenceType] and
        (__ \ "countryOfBirth").read[Country] and
        (__ \ "nationality").readNullable[Country]
      ) (PersonResidenceType.apply _)
  }

  implicit val jsonWrite: Writes[PersonResidenceType] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      __.write[ResidenceType] and
        (__ \ "countryOfBirth").write[Country] and
        (__ \ "nationality").write[Option[Country]]
      ) (unlift(PersonResidenceType.unapply))
  }

  implicit def convert(s: PersonResidenceType): Option[ResponsiblePeople] =
    Some(ResponsiblePeople(None, Some(s), None))
}
