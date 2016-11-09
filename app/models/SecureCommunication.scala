package models

import org.joda.time.LocalDate
import play.api.libs.json.Json

case class SecureCommunication(
                                status: Option[String],
                                messageType: Option[MessageType],
                                referenceNumber: Option[String],
                                isVariation: Boolean,
                                dateReceived: LocalDate,
                                isRead: Boolean)

object SecureCommunication{
  implicit val format = Json.format[SecureCommunication]
}