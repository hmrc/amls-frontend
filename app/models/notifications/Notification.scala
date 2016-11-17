package models.notifications

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.i18n.Messages

case class Notification(
                         status: Option[String],
                         contactType: Option[ContactType],
                         contactNumber: Option[String],
                         isVariation: Boolean,
                         receivedAt: DateTime
                         ) {

  def subject: String = {
    contactType match {
      case Some(msg) => Messages(s"notifications.subject.$msg")
      case None if isVariation => Messages("notifications.subject.Approval.Variation")
      case _ => Messages("notifications.subject.FailToPay")
    }
  }

  def dateReceived = {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM Y")
    receivedAt.toString(fmt)
  }

  def isRead: Boolean = false

}

object Notification
