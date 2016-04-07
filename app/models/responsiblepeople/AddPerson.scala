package models.responsiblepeople

import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._

case class AddPerson(firstName: String,
                     middleName: Option[String],
                     lastName: String,
                     isKnownByOtherNames: IsKnownByOtherNames
                    )

object AddPerson {

  import play.api.libs.json._

  val maxNameTypeLength = 35

  val firstNameType = notEmpty.withMessage("error.required.firstname") compose
    maxLength(maxNameTypeLength).withMessage("error.invalid.length.firstname")

  val middleNameType = maxLength(maxNameTypeLength).withMessage("error.invalid.length.middlename")

  val lastNameType = notEmpty.withMessage("error.required.lastname") compose
    maxLength(maxNameTypeLength).withMessage("error.invalid.length.lastname")

  implicit val formRule: Rule[UrlFormEncoded, AddPerson] = From[UrlFormEncoded] { __ =>

    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "firstName").read(firstNameType) and
        (__ \ "middleName").read(optionR(middleNameType)) and
        (__ \ "lastName").read(lastNameType) and
        (__).read[IsKnownByOtherNames]
      ) (AddPerson.apply _)
  }

  implicit val formWrites: Write[AddPerson, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "firstName").write[String] and
        (__ \ "middleName").write[Option[String]] and
        (__ \ "lastName").write[String] and
        (__).write[IsKnownByOtherNames]
      ) (unlift(AddPerson.unapply))
  }

  implicit val jsonReads: Reads[AddPerson] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (
      (__ \ "firstName").read[String] and
        (__ \ "middleName").read[Option[String]] and
        (__ \ "lastName").read[String] and
        (__).read[IsKnownByOtherNames]
      ) (AddPerson.apply _)

  }

  implicit val jsonWrites: Writes[AddPerson] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    (
      (__ \ "firstName").write[String] and
        (__ \ "middleName").write[Option[String]] and
        (__ \ "lastName").write[String] and
        (__).write[IsKnownByOtherNames]
      ) (unlift(AddPerson.unapply))
  }

}
