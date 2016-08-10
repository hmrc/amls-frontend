package models.enrolment

import play.api.libs.json.Json

case class EnrolmentIdentifier(key: String, value: String)

object EnrolmentIdentifier {
  implicit val format = Json.format[EnrolmentIdentifier]
}

case class GovernmentGatewayEnrolment(key: String, identifiers: List[EnrolmentIdentifier], state: String)

object GovernmentGatewayEnrolment {
  implicit val format = Json.format[GovernmentGatewayEnrolment]
}
