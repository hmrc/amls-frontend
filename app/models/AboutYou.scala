package models

import play.api.libs.json.Json

object YourName{
  implicit val formats = Json.format[YourName]
}

case class YourName(firstName: String, middleName: String, lastName: String)

object RoleWithinBusiness{
  implicit val formats = Json.format[RoleWithinBusiness]
}

case class RoleWithinBusiness(roleWithinBusiness: String, other: String)
