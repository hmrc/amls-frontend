package models.securecommunications

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.i18n.Messages

case class SecureCommunication(
                                status: Option[String],
                                messageType: Option[MessageType],
                                referenceNumber: Option[String],
                                isVariation: Boolean,
                                timeReceived: DateTime,
                                isRead: Boolean) {

  def subject: String = {
    messageType match {
      case Some(msg) => Messages(s"secure.communications.subject.$msg")
      case None if isVariation => Messages("secure.communications.subject.approval.variation")
      case _ => Messages("secure.communications.subject.failtopay")
    }
  }

  def dateReceived = {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM Y")
    timeReceived.toString(fmt)
  }

}

object SecureCommunication
