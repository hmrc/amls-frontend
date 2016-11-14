package models.securecommunications

import org.joda.time.{DateTime, LocalDate}
import play.api.i18n.Messages

case class SecureCommunication(status: Option[String], messageType: Option[MessageType], referenceNumber: Option[String], isVariation: Boolean, timeReceived: DateTime, isRead: Boolean) {

  def subject: String = {
    messageType match {
      case Some(msg) => Messages(s"secure.communications.subject.$msg")
      case None if isVariation => Messages("secure.communications.subject.approval.variation")
      case _ => Messages("secure.communications.subject.failtopay")
    }
  }

}

object SecureCommunication
