package utils

import models.notifications.ContactType.NoSubject
import models.notifications._
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.PlaySpec

class ContactTypeHelperSpec extends PlaySpec with AmlsSpec {

  "ContactTypeHelper " must {
    "return correct ContactType" when {

      "contact type is populated" in {
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
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff"
          ))

        ContactTypeHelper.getContactType(model.status, model.contactType, model.variation) mustBe ContactType.MindedToRevoke
      }

      "auto rejected" in {
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
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff"
          ))

        ContactTypeHelper.getContactType(model.status, model.contactType, model.variation) mustBe ContactType.ApplicationAutorejectionForFailureToPay
      }

      "variation approved" in {
        val model = NotificationRow(
          Some(
            Status(
              Some(StatusType.Approved), None
            )),
          None,
          None,
          true,
          new DateTime(1479730062573L, DateTimeZone.UTC),
          false,
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff"
          ))

        ContactTypeHelper.getContactType(model.status, model.contactType, model.variation) mustBe ContactType.RegistrationVariationApproval
      }

      "DeRegistrationEffectiveDateChange" in {
        val model = NotificationRow(
          Some(
            Status(
              Some(StatusType.DeRegistered), None
            )),
          None,
          None,
          true,
          new DateTime(1479730062573L, DateTimeZone.UTC),
          false,
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff"
          ))

        ContactTypeHelper.getContactType(model.status, model.contactType, model.variation) mustBe ContactType.DeRegistrationEffectiveDateChange
      }

      "return NoSubject when ContactType not determined" in {
        val model = NotificationRow(
          Some(
            Status(
              Some(StatusType.Approved), None
            )),
          None,
          None,
          false,
          new DateTime(1479730062573L, DateTimeZone.UTC),
          false,
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff"
          ))

        ContactTypeHelper.getContactType(model.status, model.contactType, model.variation) mustBe NoSubject
        model.subject mustBe "notifications.subject.NoSubject"

      }
    }
  }
}
