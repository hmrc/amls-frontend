package models.declaration

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}

case class AddPerson(firstName: String,
                     middleName: Option[String],
                     lastName: String,
                     roleWithinBusiness: RoleWithinBusiness
                    )

object AddPerson {

  import play.api.libs.json._

  val key = "add-person"

  implicit val formRule: Rule[UrlFormEncoded, AddPerson] = From[UrlFormEncoded] { __ =>
    import models.FormTypes._
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "firstName").read(declarationNameType) and
        (__ \ "middleName").read(optionR(declarationNameType)) and
        (__ \ "lastName").read(declarationNameType) and
        __.read[RoleWithinBusiness]
      ) (AddPerson.apply _)
  }

  implicit val formWrites: Write[AddPerson, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "firstName").write[String] and
        (__ \ "middleName").write[Option[String]] and
        (__ \ "lastName").write[String] and
        __.write[RoleWithinBusiness]
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
