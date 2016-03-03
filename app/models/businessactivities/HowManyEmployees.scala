package models.businessactivities

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.Json

case class HowManyEmployees(employeeCount: String,
                            employeeCountAMLSSupervision: String)


object HowManyEmployees {

  implicit val formats = Json.format[HowManyEmployees]

  implicit val formWrites: Write[HowManyEmployees, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "employeeCount").write[String] and
        (__ \ "employeeCountAMLSSupervision").write[String]
      ) (unlift(HowManyEmployees.unapply _))
  }

  implicit val formRule: Rule[UrlFormEncoded, HowManyEmployees] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "employeeCount").read(notEmpty) and
          (__ \ "employeeCountAMLSSupervision").read(notEmpty)
        ) (HowManyEmployees.apply _)
    }

}