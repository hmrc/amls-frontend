package models.businessactivities

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.Json
import models.FormTypes._

case class HowManyEmployees(employeeCount: String,
                            employeeCountAMLSSupervision: String)


object HowManyEmployees {

  implicit val formats = Json.format[HowManyEmployees]

  val employeeCountRegex = "^[0-9]+$".r
  val maxEmployeeCount = 11
  val employeeCountType = notEmptyStrip compose
                          customMaxLength(maxEmployeeCount, "error.max.length.ba.employee.count") compose
                          customRegex(employeeCountRegex, "error.invalid.ba.employee.count")


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

      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "employeeCount").read(customNotEmpty("error.required.ba.employee.count1") compose employeeCountType) and
          (__ \ "employeeCountAMLSSupervision").read(customNotEmpty("error.required.ba.employee.count2") compose employeeCountType)
        ) (HowManyEmployees.apply _)
    }

}