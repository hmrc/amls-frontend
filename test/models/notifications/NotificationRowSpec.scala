package models.notifications

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import org.specs2.mock.mockito.MockitoMatchers
import ContactType._
import play.api.data.validation.ValidationError
import play.api.libs.json._

class NotificationRowSpec extends PlaySpec with MockitoMatchers with OneAppPerSuite {

  val testNotifications = NotificationRow(
    Some(
      Status(Some(StatusType.Revoked),
        Some(RevokedReason.RevokedCeasedTrading))
    ),
    None,
    None,
    false,
    DateTime.now(),
    IDType("1234567")
  )

  "Notification Contact types" must {
    "retrieve the corresponding subject from messages" when {
      "message type is APA1" in {
        testNotifications.copy(contactType = Some(ApplicationApproval)).subject mustBe "Application Approval"
      }
      "message type is not given and variation is true" in {
        testNotifications.copy(variation = true).subject mustBe "Registration Variation Approval"
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

    "read/write Json successfully" in {
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RejectionReasons)) must be(JsSuccess(ContactType.RejectionReasons))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RevocationReasons)) must be(JsSuccess(ContactType.RevocationReasons))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.MindedToReject)) must be(JsSuccess(ContactType.MindedToReject))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.NoLongerMindedToReject)) must be(JsSuccess(ContactType.NoLongerMindedToReject))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.MindedToRevoke)) must be(JsSuccess(ContactType.MindedToRevoke))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.NoLongerMindedToRevoke)) must be(JsSuccess(ContactType.NoLongerMindedToRevoke))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.Others)) must be(JsSuccess(ContactType.Others))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ApplicationApproval)) must be(JsSuccess(ContactType.ApplicationApproval))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RenewalApproval)) must be(JsSuccess(ContactType.RenewalApproval))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.AutoExpiryOfRegistration)) must be(JsSuccess(ContactType.AutoExpiryOfRegistration))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RenewalReminder)) must be(JsSuccess(ContactType.RenewalReminder))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForApplication)) must be(JsSuccess(ContactType.ReminderToPayForApplication))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForRenewal)) must be(JsSuccess(ContactType.ReminderToPayForRenewal))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForVariation)) must be(JsSuccess(ContactType.ReminderToPayForVariation))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForManualCharges)) must be(JsSuccess(ContactType.ReminderToPayForManualCharges))
    }

    "fail with error when status value is passed incorrectly" in {
      ContactType.jsonReads.reads(JsString("RPM1RPM1")) must be(JsError(List((JsPath  \"contact_type",List(ValidationError("error.invalid"))))))
    }

    "format the date for the table of messages" in {
      testNotifications.copy(receivedAt = new DateTime(2017, 12, 1, 3, 3, DateTimeZone.UTC)).dateReceived mustBe "1 December 2017"
    }

    "read and write json successfully"  in {
      val model = NotificationRow(
        Some(
          Status(
            Some(StatusType.Revoked),
            Some(RevokedReason.RevokedCeasedTrading)
          )),
        Some(ContactType.MindedToRevoke),
        None,
        false,
        new DateTime(1479730062573L, DateTimeZone.UTC),
        new IDType("5832e38e01000001005ca3ff"
        ))

      val json = Json.parse(
        """
          |{"status":{"status_type":"08","status_reason":"02"},"contactType":"MTRV","variation":false,"receivedAt":{"$date":1479730062573},"_id":{"$oid":"5832e38e01000001005ca3ff"}}
          |
        """.stripMargin)

      NotificationRow.format.reads(json) must be(JsSuccess(model))
    }
  }

}
