package models.notifications

import play.api.i18n.Messages

trait SubjectBuilder {
  val contactType: Option[ContactType]
  val variation: Boolean

  def subject = {
    contactType match {
      case Some(msg) => s"notifications.subject.$msg"
      case None if variation => "notifications.subject.Approval.Variation"
      case _ => "notifications.subject.FailToPay"
    }
  }
}
