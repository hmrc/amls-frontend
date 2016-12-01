package models.notifications

import play.api.libs.json.Json

case class NotificationDetails(contactType : Option[ContactType],
                               status : Option[Status],
                               messageText : Option[String],
                               variation : Boolean) extends SubjectBuilder

object NotificationDetails {
  implicit val reads = Json.reads[NotificationDetails]
}
