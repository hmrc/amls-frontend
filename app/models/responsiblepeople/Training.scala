/*
package models.responsiblepeople

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._

case class Training(isKnownByOtherNames: IsKnownByOtherNames)

object Training {

  import play.api.libs.json._

  val maxNameTypeLength = 35

  implicit val formRule: Rule[UrlFormEncoded, AddPerson] = From[UrlFormEncoded] { __ =>
    (
      (__).read[IsKnownByOtherNames]
      ) (Training.apply _)
  }

  implicit val formWrites: Write[AddPerson, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.libs.functional.syntax.unlift
    (
      (__).write[IsKnownByOtherNames]
      ) (unlift(Training.unapply))
  }

  implicit val jsonReads: Reads[AddPerson] = {
    import play.api.libs.json._
    (
      (__).read[IsKnownByOtherNames]
      ) (Training.apply _)

  }

  implicit val jsonWrites: Writes[AddPerson] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__).write[IsKnownByOtherNames]
      ) (unlift(Training.unapply))
  }

}
*/
