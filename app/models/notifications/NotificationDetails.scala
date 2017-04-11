package models.notifications

import models.confirmation.Currency
import models.notifications.ContactType.{RegistrationVariationApproval, ApplicationAutorejectionForFailureToPay, DeRegistrationEffectiveDateChange}
import models.notifications.StatusType.DeRegistered
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.joda.time.LocalDate
import play.api.libs.json.Json

case class NotificationDetails(contactType : Option[ContactType],
                               status : Option[Status],
                               messageText : Option[String],
                               variation : Boolean) {

  def getContactType: ContactType = {

    val statusReason = for {
      st <- status
      reason <- st.statusReason
    } yield reason

    contactType.getOrElse(
      (status, statusReason, variation) match {
        case (Some(Status(Some(DeRegistered),_)),_,_) => DeRegistrationEffectiveDateChange
        case (_, Some(r),_) => ApplicationAutorejectionForFailureToPay
        case (_,_, true) => RegistrationVariationApproval
        case _ => throw new RuntimeException("No matching ContactType found")
      }
    )
  }

  def subject = {
    s"notifications.subject.$getContactType"
  }
}

object NotificationDetails {

  val dateTimeFormat = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC

  def convertEndDateWithRefMessageText(inputString: String): Option[EndDateDetails] = {

    inputString.split("\\|").toList match {
      case date::ref::tail => convertMessageTextWithRefNo(date, ref)
      case d if d.length == 1 => {
        convertEndDateMessageText(d.head)
      }
      case _ => None
    }
  }

  def convertMessageTextWithRefNo(date: String, ref: String): Some[EndDateDetails] = {
    val dateValue = LocalDate.parse(splitByDash(date), DateTimeFormat.forPattern("dd/MM/yyyy"))
    Some(EndDateDetails(dateValue, Some(splitByDash(ref))))
  }

  def convertEndDateMessageText(inputString: String): Option[EndDateDetails] = {
    inputString.split("-") match {
      case dateString if dateString.length > 1 => {
        val dateValue = LocalDate.parse(dateString(1), DateTimeFormat.forPattern("dd/MM/yyyy"))
        Some(EndDateDetails(dateValue, None))
      }
      case _ => None
    }
  }

  def convertReminderMessageText(inputString: String): Option[ReminderDetails] = {
    inputString.split("\\|").toList match {
      case amount::ref::tail =>
        Some(ReminderDetails(Currency(splitByDash(amount).toDouble),splitByDash(ref)))
      case _ => None
    }
  }

  private def splitByDash(s: String): String = s.split("-")(1)

  implicit val reads = Json.reads[NotificationDetails]
}
