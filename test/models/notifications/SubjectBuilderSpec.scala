package models.notifications

import models.notifications.ContactType._
import org.scalatest.WordSpec
import org.scalatest.MustMatchers


class SubjectBuilderSpec extends WordSpec with MustMatchers{
  "SubjectBuilder" must {
    case class TestableSubjectBuilder(contactType: Option[ContactType], variation: Boolean) extends SubjectBuilder

    val testableSubjectBuilder = new TestableSubjectBuilder(None, false)

    "retrieve the corresponding subject from messages" when {
      "message type is APA1" in {

        testableSubjectBuilder.copy(contactType = Some(ApplicationApproval)).subject mustBe "notifications.subject.ApplicationApproval"
      }
      "message type is not given and variation is true" in {
        testableSubjectBuilder.copy(variation = true).subject mustBe "notifications.subject.Approval.Variation"
      }
      "message type is not given and variation is false" in {
        testableSubjectBuilder.subject mustBe "notifications.subject.FailToPay"
      }
      "message type is APR1" in {
        testableSubjectBuilder.copy(contactType = Some(RenewalApproval)).subject mustBe "notifications.subject.RenewalApproval"
      }
      "message type is REJR" in {
        testableSubjectBuilder.copy(contactType = Some(RejectionReasons)).subject mustBe "notifications.subject.RejectionReasons"
      }

      "message type is REVR" in {
        testableSubjectBuilder.copy(contactType = Some(RevocationReasons)).subject mustBe "notifications.subject.RevocationReasons"
      }

      "message type is EXPR" in {
        testableSubjectBuilder.copy(contactType = Some(AutoExpiryOfRegistration)).subject mustBe "notifications.subject.AutoExpiryOfRegistration"
      }

      "message type is RPA1" in {
        testableSubjectBuilder.copy(contactType = Some(ReminderToPayForApplication)).subject mustBe "notifications.subject.ReminderToPayForApplication"
      }

      "message type is RPV1" in {
        testableSubjectBuilder.copy(contactType = Some(ReminderToPayForVariation)).subject mustBe "notifications.subject.ReminderToPayForVariation"
      }

      "message type is RPR1" in {
        testableSubjectBuilder.copy(contactType = Some(ReminderToPayForRenewal)).subject mustBe "notifications.subject.ReminderToPayForRenewal"
      }
      "message type is RPM1" in {
        testableSubjectBuilder.copy(contactType = Some(ReminderToPayForManualCharges)).subject mustBe "notifications.subject.ReminderToPayForManualCharges"
      }
      "message type is RREM" in {
        testableSubjectBuilder.copy(contactType = Some(RenewalReminder)).subject mustBe "notifications.subject.RenewalReminder"
      }
      "message type is MTRJ" in {
        testableSubjectBuilder.copy(contactType = Some(MindedToReject)).subject mustBe "notifications.subject.MindedToReject"
      }
      "message type is MTRV" in {
        testableSubjectBuilder.copy(contactType = Some(MindedToRevoke)).subject mustBe "notifications.subject.MindedToRevoke"
      }
      "message type is NMRJ" in {
        testableSubjectBuilder.copy(contactType = Some(NoLongerMindedToReject)).subject mustBe "notifications.subject.NoLongerMindedToReject"
      }
      "message type is NMRV" in {
        testableSubjectBuilder.copy(contactType = Some(NoLongerMindedToRevoke)).subject mustBe "notifications.subject.NoLongerMindedToRevoke"
      }
      "message type is OTHR" in {
        testableSubjectBuilder.copy(contactType = Some(Others)).subject mustBe "notifications.subject.Others"
      }
    }
  }
}
