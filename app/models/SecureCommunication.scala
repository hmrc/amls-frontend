package models

import org.joda.time.LocalDate
import play.api.i18n.Messages

case class SecureCommunication(
                                status: Option[String],
                                messageType: Option[MessageType],
                                referenceNumber: Option[String],
                                isVariation: Boolean,
                                dateReceived: LocalDate,
                                isRead: Boolean) {

  def subject(): String = {
    messageType match {
      case Some(msg) => Messages(s"secure.communications.subject.$msg")
      case None if isVariation => Messages("secure.communications.subject.approval.variation")
      case _ => Messages("secure.communications.subject.failtopay")
    }
  }

}

object SecureCommunication{
  
}