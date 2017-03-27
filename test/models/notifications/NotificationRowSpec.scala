package models.notifications

import jto.validation.ValidationError
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class NotificationRowSpec extends PlaySpec {

  val testNotifications = NotificationRow(
    Some(
      Status(Some(StatusType.Revoked),
        Some(RevokedReason.RevokedCeasedTrading))
    ),
    None,
    None,
    false,
    DateTime.now(),
    false,
    IDType("1234567")
  )

  "Notification Contact types" must {
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
      ContactType.jsonReads.reads(JsString("RPM1RPM1")) must be(JsError(List((JsPath  \"contact_type",List(play.api.data.validation.ValidationError("error.invalid"))))))
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
        false,
        new IDType("5832e38e01000001005ca3ff"
        ))

      val json = Json.parse(
        """
          |{
          | "status":{
          |   "status_type":"08",
          |   "status_reason":"02"},
          | "contactType":"MTRV",
          | "variation":false,
          | "receivedAt":{"$date":1479730062573},
          | "isRead":false,
          | "_id":{"$oid":"5832e38e01000001005ca3ff"}}
          |
        """.stripMargin)

      NotificationRow.format.reads(json) must be(JsSuccess(model))
    }

    "return correct ContactType when contact type is populated" in {
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
        false,
        new IDType("5832e38e01000001005ca3ff"
        ))

      model.getContactType mustBe(ContactType.MindedToRevoke)
    }

    "return correct ContactType when auto rejected" in {
      val model = NotificationRow(
        Some(
          Status(
            Some(StatusType.Rejected),
            Some(RejectedReason.FailedToPayCharges)
          )),
        None,
        None,
        false,
        new DateTime(1479730062573L, DateTimeZone.UTC),
        false,
        new IDType("5832e38e01000001005ca3ff"
        ))

      model.getContactType mustBe(ContactType.ApplicationAutorejectionForFailureToPay)
    }

    "return correct ContactType when variation approved" in {
      val model = NotificationRow(
        Some(
          Status(
            Some(StatusType.Approved),None
          )),
        None,
        None,
        true,
        new DateTime(1479730062573L, DateTimeZone.UTC),
        false,
        new IDType("5832e38e01000001005ca3ff"
        ))

      model.getContactType mustBe(ContactType.RegistrationVariationApproval)
    }

    "return correct ContactType when DeRegistrationEffectiveDateChange" in {
      val model = NotificationRow(
        Some(
          Status(
            Some(StatusType.DeRegistered),None
          )),
        None,
        None,
        true,
        new DateTime(1479730062573L, DateTimeZone.UTC),
        false,
        new IDType("5832e38e01000001005ca3ff"
        ))

      model.getContactType mustBe(ContactType.DeRegistrationEffectiveDateChange)
    }

    "throw an error when ContactType not determined" in {
      val model = NotificationRow(
        Some(
          Status(
            Some(StatusType.Approved),None
          )),
        None,
        None,
        false,
        new DateTime(1479730062573L, DateTimeZone.UTC),
        false,
        new IDType("5832e38e01000001005ca3ff"
        ))

      intercept[RuntimeException] {
        model.getContactType
      }

    }
  }

}
