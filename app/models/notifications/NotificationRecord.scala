package models.notifications

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.i18n.Messages

case class NotificationRecord(
                                status: Option[String],
                                messageType: Option[MessageType],
                                referenceNumber: Option[String],
                                isVariation: Boolean,
                                timeReceived: DateTime,
                                isRead: Boolean) {

  def subject: String = {
    messageType match {
      case Some(msg) => Messages(s"notifications.subject.$msg")
      case None if isVariation => Messages("notifications.subject.approval.variation")
      case _ => Messages("notifications.subject.failtopay")
    }
  }

  def dateReceived = {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM Y")
    timeReceived.toString(fmt)
  }
}