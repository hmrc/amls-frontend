package models

import play.api.libs.json.Json

object YourName{
  implicit val formats = Json.format[YourName]
}

case class YourName(firstName: String, middleName:  Option[String], lastName: String)

case class RoleWithinBusiness(roleWithinBusiness: String, other: String)

object RoleWithinBusiness{
  implicit val formats = Json.format[RoleWithinBusiness]
}


object RoleForBusiness{
  implicit val formats = Json.format[RoleForBusiness]
}

case class RoleForBusiness(roleForBusiness: String, other: String)
