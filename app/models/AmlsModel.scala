package models

import play.api.libs.json.Json

object LoginDetails{
  implicit val formats = Json.format[LoginDetails]
}

case class LoginDetails(name: String, password: String) 

object YourName{
  implicit val formats = Json.format[YourName]
}

case class YourName(firstName: String, middleName: String, lastName: String)

