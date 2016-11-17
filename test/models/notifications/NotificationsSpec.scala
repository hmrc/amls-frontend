package models.notifications

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import org.specs2.mock.mockito.MockitoMatchers

class NotificationsSpec extends PlaySpec with MockitoMatchers with OneAppPerSuite {

  val testNotifications = Notification(None, None, None, false, new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))

  "Secure Communication" must {
    "retrieve the corresponding subject from messages" when {
      "message type is APA1" in {
        testNotifications.copy(contactType = Some(ApplicationApproval)).subject mustBe "Application Approval"
      }
      "message type is not given and variation is true" in {
        testNotifications.copy(isVariation = true).subject mustBe "Registration Variation Approval"
      }
      "message type is not given and variation is false" in {
        testNotifications.subject mustBe "Application Auto-Rejection for Failure to Pay"
      }
      "message type is APR1" in {
        testNotifications.copy(contactType = Some(RenewalApproval)).subject mustBe "Renewal Approval"
      }
      "message type is REJR" in {
        testNotifications.copy(contactType = Some(RejectionReasons)).subject mustBe "Application Rejection"
      }

      "message type is REVR" in {
        testNotifications.copy(contactType = Some(RevocationReasons)).subject mustBe "Registration Revocation"
      }

      "message type is EXPR" in {
        testNotifications.copy(contactType = Some(AutoExpiryOfRegistration)).subject mustBe "Registration Expiry"
      }

      "message type is RPA1" in {
        testNotifications.copy(contactType = Some(ReminderToPayForApplication)).subject mustBe "Reminder to Pay - Application"
      }

      "message type is RPV1" in {
        testNotifications.copy(contactType = Some(ReminderToPayForVariation)).subject mustBe "Reminder to Pay - Variation"
      }

      "message type is RPR1" in {
        testNotifications.copy(contactType = Some(ReminderToPayForRenewal)).subject mustBe "Reminder to Pay - Renewal"
      }
      "message type is RPM1" in {
        testNotifications.copy(contactType = Some(ReminderToPayForManualCharges)).subject mustBe "Reminder to Pay - Manual Charge"
      }
      "message type is RREM" in {
        testNotifications.copy(contactType = Some(RenewalReminder)).subject mustBe "Reminder to Renew"
      }
      "message type is MTRJ" in {
        testNotifications.copy(contactType = Some(MindedToReject)).subject mustBe "Rejection of Application being Considered"
      }
      "message type is MTRV" in {
        testNotifications.copy(contactType = Some(MindedToRevoke)).subject mustBe "Revocation of Registration being Considered"
      }
      "message type is NMRJ" in {
        testNotifications.copy(contactType = Some(NoLongerMindedToReject)).subject mustBe "Rejection of Application no longer being Considered"
      }
      "message type is NMRV" in {
        testNotifications.copy(contactType = Some(NoLongerMindedToRevoke)).subject mustBe "Revocation of Registration no longer being Considered"
      }
      "message type is OTHR" in {
        testNotifications.copy(contactType = Some(Others)).subject mustBe "Generic communication"
      }
    }
    "format the date for the table of messages" in {
      testNotifications.dateReceived mustBe "1 December 2017"
    }
  }

}
