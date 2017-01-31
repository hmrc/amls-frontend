package models.declaration

import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}

case class AddPerson(firstName: String,
                     middleName: Option[String],
                     lastName: String,
                     roleWithinBusiness: RoleWithinBusiness
                    )

object AddPerson {

  import play.api.libs.json._

  val key = "add-person"

  //TODO: Update these read types to use correct name types.
  implicit val formRule: Rule[UrlFormEncoded, AddPerson] = From[UrlFormEncoded] { __ =>
    import models.FormTypes._
    import jto.validation.forms.Rules._
    (
      (__ \ "firstName").read(declarationNameType) ~
        (__ \ "middleName").read(optionR(declarationNameType)) ~
        (__ \ "lastName").read(declarationNameType) ~
        __.read[RoleWithinBusiness]
      ) (AddPerson.apply _)
  }

  implicit val formWrites: Write[AddPerson, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "firstName").write[String] ~
        (__ \ "middleName").write[Option[String]] ~
        (__ \ "lastName").write[String] ~
        __.write[RoleWithinBusiness]
      ) (unlift(AddPerson.unapply))
  }

  implicit val jsonReads: Reads[AddPerson] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (
      (__ \ "firstName").read[String] and
        (__ \ "middleName").readNullable[String] and
        (__ \ "lastName").read[String] and
        (__.read[RoleWithinBusiness])
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
        __.write[RoleWithinBusiness]
      ) (unlift(AddPerson.unapply))
  }


}
