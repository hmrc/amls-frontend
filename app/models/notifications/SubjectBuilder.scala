package models.notifications

import models.notifications.ContactType.{ApplicationAutorejectionForFailureToPay, RegistrationVariationApproval}
import play.api.i18n.Messages

trait SubjectBuilder {
  val contactType: Option[ContactType]
  val variation: Boolean


  def getContactType: ContactType

  def subject = {
    println("**************" + contactType)
    getContactType match {
      case RegistrationVariationApproval => "notifications.subject.Approval.Variation"
      case ApplicationAutorejectionForFailureToPay => "notifications.subject.FailToPay"
      case x => s"notifications.subject.$x"
    }
  }
}
