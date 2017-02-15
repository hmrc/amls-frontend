package models.declaration

import config.ApplicationConfig
import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}
import models.declaration.release7.{RoleType, RoleWithinBusinessRelease7}

case class AddPerson(firstName: String,
                     middleName: Option[String],
                     lastName: String,
                     roleWithinBusiness: RoleWithinBusinessRelease7
                    )

object AddPerson {

  import play.api.libs.json._

  val key = "add-person"

  implicit def convert(role: RoleWithinBusiness): RoleType = {
    role match {
      case BeneficialShareholder => models.declaration.release7.BeneficialShareholder
      case Director => models.declaration.release7.Director
      case Partner => models.declaration.release7.Partner
      case InternalAccountant => models.declaration.release7.InternalAccountant
      case ExternalAccountant => models.declaration.release7.ExternalAccountant
      case SoleProprietor => models.declaration.release7.SoleProprietor
      case NominatedOfficer => models.declaration.release7.NominatedOfficer
      case BeneficialShareholder => models.declaration.release7.BeneficialShareholder
      case Other(x) => models.declaration.release7.Other(x)
    }
  }



  //TODO: Update these read types to use correct name types.
  implicit def formRule: Rule[UrlFormEncoded, AddPerson] = From[UrlFormEncoded] { __ =>
    import models.FormTypes._
    import jto.validation.forms.Rules._

    val roleReader: Rule[UrlFormEncoded, RoleWithinBusinessRelease7] = {
      ApplicationConfig.release7 match {
        case false => __.read[RoleWithinBusiness] map { v => RoleWithinBusinessRelease7.apply(Set(v)) }
        case _ => __.read[RoleWithinBusinessRelease7]
      }
    }

    (
      (__ \ "firstName").read(declarationNameType) ~
        (__ \ "middleName").read(optionR(declarationNameType)) ~
        (__ \ "lastName").read(genericNameRule("error.required.declaration.last_name")) ~
        roleReader
      ) (AddPerson.apply)
  }

  implicit val formWrites: Write[AddPerson, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "firstName").write[String] ~
        (__ \ "middleName").write[Option[String]] ~
        (__ \ "lastName").write[String] ~
        __.write[RoleWithinBusinessRelease7]
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
        (__.read[RoleWithinBusinessRelease7])
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
        __.write[RoleWithinBusinessRelease7]
      ) (unlift(AddPerson.unapply))
  }


}
