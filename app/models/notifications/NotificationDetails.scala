package models.notifications

import models.notifications.ContactType.{RegistrationVariationApproval, ApplicationAutorejectionForFailureToPay, DeRegistrationEffectiveDateChange}
import models.notifications.StatusType.DeRegistered
import play.api.libs.json.Json

case class NotificationDetails(contactType : Option[ContactType],
                               status : Option[Status],
                               messageText : Option[String],
                               variation : Boolean) extends SubjectBuilder {

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

}

object NotificationDetails {
  implicit val reads = Json.reads[NotificationDetails]
}
