package models.governmentgateway

import play.api.libs.json.Json

case class EnrolmentResponse(
                       serviceName: String,
                       state: String,
                       friendlyName: String,
                       identifiersForDisplay: Seq[Identifier]
                       )

object EnrolmentResponse {
  implicit val format = Json.format[EnrolmentResponse]
}
