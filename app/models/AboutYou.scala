package models

import play.api.libs.json.Json

case class YourDetails(firstName: String, middleName: Option[String], lastName: String)

object YourDetails {
  implicit val formats = Json.format[YourDetails]
}

case class EmployedWithinTheBusiness(isEmployed: Boolean)

object EmployedWithinTheBusiness {
  implicit val formats = Json.format[EmployedWithinTheBusiness]
}

case class RoleWithinBusiness(roleWithinBusiness: String, other: String)

object RoleWithinBusiness {
  implicit val formats = Json.format[RoleWithinBusiness]
}

case class RoleForBusiness(roleForBusiness: String, other: String)

object RoleForBusiness {
  implicit val formats = Json.format[RoleForBusiness]
}
