package models

import play.api.libs.json.Json

/**
 * Created by user on 20/08/15.
 */
object LoginDetails{
  implicit val formats = Json.format[LoginDetails]
}

case class LoginDetails(name: String, password: String)