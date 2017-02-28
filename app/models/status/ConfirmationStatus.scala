package models.status

import play.api.libs.json.Json

case class ConfirmationStatus(submissionConfirmed: Option[Boolean])

object ConfirmationStatus {

  val key = "Submission_Indicator"

  implicit val format = Json.format[ConfirmationStatus]
}
