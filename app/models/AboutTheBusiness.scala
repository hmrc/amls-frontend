package models

import play.api.libs.json.Json

case class TelephoningYourBusiness(businessPhoneNumber: String,
                                   mobileNumber: Option[String])

object TelephoningYourBusiness {
  implicit val formats = Json.format[TelephoningYourBusiness]
}
