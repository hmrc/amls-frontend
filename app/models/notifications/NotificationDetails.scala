package models.notifications

case class NotificationDetails(contactType : Option[MessageType],
                               status : Option[String],
                               statusReason : Option[String],
                               messageText : Option[String])
