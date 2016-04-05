package models.businessactivities

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.Json
import models.FormTypes._

import play.api.data.mapping.forms.Rules._
import utils.MappingUtils.Implicits._

case class HowManyEmployees(employeeCount: String,
                            employeeCountAMLSSupervision: String)


object HowManyEmployees {

  implicit val formats = Json.format[HowManyEmployees]

  val employeeCountRegex = "^[0-9]+$".r
  val maxEmployeeCount = 11
  val employeeCountType = notEmptyStrip compose maxLength(maxEmployeeCount).withMessage("error.max.length.ba.employee.count") compose
                          pattern(employeeCountRegex).withMessage("error.invalid.ba.employee.count")


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
      (
        (__ \ "employeeCount").read(notEmpty.withMessage("error.required.ba.employee.count1") compose employeeCountType) and
          (__ \ "employeeCountAMLSSupervision").read(notEmpty.withMessage("error.required.ba.employee.count2") compose employeeCountType)
        ) (HowManyEmployees.apply _)
    }

}