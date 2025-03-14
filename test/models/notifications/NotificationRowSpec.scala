/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.notifications

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import utils.AmlsSpec

import java.time.{Instant, LocalDateTime}
import java.time.ZoneOffset.UTC

class NotificationRowSpec extends PlaySpec with AmlsSpec {

  val testNotifications = NotificationRow(
    Some(
      Status(Some(StatusType.Revoked), Some(RevokedReason.RevokedCeasedTrading))
    ),
    None,
    None,
    false,
    LocalDateTime.now(),
    false,
    "XJML00000200000",
    "v1m0",
    IDType("1234567")
  )

  "NotificationRows " must {
    "read/write Contact types Json successfully" in {
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RejectionReasons))              must be(
        JsSuccess(ContactType.RejectionReasons)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RevocationReasons))             must be(
        JsSuccess(ContactType.RevocationReasons)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.MindedToReject))                must be(
        JsSuccess(ContactType.MindedToReject)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.NoLongerMindedToReject))        must be(
        JsSuccess(ContactType.NoLongerMindedToReject)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.MindedToRevoke))                must be(
        JsSuccess(ContactType.MindedToRevoke)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.NoLongerMindedToRevoke))        must be(
        JsSuccess(ContactType.NoLongerMindedToRevoke)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.Others))                        must be(
        JsSuccess(ContactType.Others)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ApplicationApproval))           must be(
        JsSuccess(ContactType.ApplicationApproval)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RenewalApproval))               must be(
        JsSuccess(ContactType.RenewalApproval)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.AutoExpiryOfRegistration))      must be(
        JsSuccess(ContactType.AutoExpiryOfRegistration)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RenewalReminder))               must be(
        JsSuccess(ContactType.RenewalReminder)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForApplication))   must be(
        JsSuccess(ContactType.ReminderToPayForApplication)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForRenewal))       must be(
        JsSuccess(ContactType.ReminderToPayForRenewal)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForVariation))     must be(
        JsSuccess(ContactType.ReminderToPayForVariation)
      )
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForManualCharges)) must be(
        JsSuccess(ContactType.ReminderToPayForManualCharges)
      )
    }

    "fail with error when status value is passed incorrectly" in {
      ContactType.jsonReads.reads(JsString("RPM1RPM1")) must be(
        JsError(List((JsPath \ "contact_type", List(play.api.libs.json.JsonValidationError("error.invalid")))))
      )
    }

    "format the date for the table of messages" in {
      testNotifications.copy(receivedAt = LocalDateTime.of(2017, 12, 1, 3, 3)).dateReceived mustBe "1 December 2017"
    }

    "read and write json successfully" ignore {
      val model = NotificationRow(
        Some(
          Status(
            Some(StatusType.Revoked),
            Some(RevokedReason.RevokedCeasedTrading)
          )
        ),
        Some(ContactType.MindedToRevoke),
        None,
        false,
        LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC),
        false,
        "XJML00000200000",
        "1",
        new IDType("5832e38e01000001005ca3ff")
      )

      val json = Json.parse("""
          |{
          | "status":{
          |   "status_type":"08",
          |   "status_reason":"02"},
          | "contactType":"MTRV",
          | "variation":false,
          | "receivedAt" : "2016-11-20T15:31:23.753Z",
          | "amlsRegistrationNumber":"XJML00000200000",
          | "isRead":false,
          | "templatePackageVersion":"1",
          | "_id":{"$oid":"5832e38e01000001005ca3ff"}}
          |
        """.stripMargin)

      NotificationRow.format.reads(json) must be(JsSuccess(model))
    }

    "return application failure subject line" when {

      val notificationRow = NotificationRow(
        None,
        Some(ContactType.RejectionReasons),
        None,
        false,
        LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC),
        false,
        "XJML00000200000",
        "1",
        new IDType("5832e38e01000001005ca3ff")
      )

      "status reason is 2" in {
        notificationRow
          .copy(
            status = Some(
              Status(
                Some(StatusType.Rejected),
                Some(RejectedReason.FailedToRespond)
              )
            )
          )
          .subject must be("notifications.fail.title")
      }

      "status reason is 3" when {
        "contact number & contact type are present" in {
          notificationRow
            .copy(
              status = Some(
                Status(
                  Some(StatusType.Rejected),
                  Some(RejectedReason.FailedToPayCharges)
                )
              )
            )
            .subject must be("notifications.fail.title")
        }

        "contact number & contact type are absent" in {
          notificationRow
            .copy(
              status = Some(
                Status(
                  Some(StatusType.Rejected),
                  Some(RejectedReason.FailedToPayCharges)
                )
              ),
              contactType = None
            )
            .subject must be("notifications.fail.title")
        }
      }

      "status reason is 98" in {
        notificationRow
          .copy(
            status = Some(
              Status(
                Some(StatusType.Rejected),
                Some(RejectedReason.OtherFailed)
              )
            )
          )
          .subject must be("notifications.fail.title")
      }

      "default" in {
        notificationRow.subject must be("notifications.fail.title")
      }

    }

    "return application refusal subject line" when {

      val notificationRow = NotificationRow(
        None,
        Some(ContactType.RejectionReasons),
        None,
        false,
        LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC),
        false,
        "XJML00000200000",
        "1",
        new IDType("5832e38e01000001005ca3ff")
      )

      "status reason is 1" in {
        notificationRow
          .copy(
            status = Some(
              Status(
                Some(StatusType.Rejected),
                Some(RejectedReason.NonCompliant)
              )
            )
          )
          .subject must be("notifications.rejr.title")
      }

      "status reason is 4" in {
        notificationRow
          .copy(
            status = Some(
              Status(
                Some(StatusType.Rejected),
                Some(RejectedReason.FitAndProperFailure)
              )
            )
          )
          .subject must be("notifications.rejr.title")
      }

      "status reason is 99" in {
        notificationRow
          .copy(
            status = Some(
              Status(
                Some(StatusType.Rejected),
                Some(RejectedReason.OtherRefused)
              )
            )
          )
          .subject must be("notifications.rejr.title")
      }

    }
  }

  "IDType" must {
    "read json successfully" in {

      IDType.read.reads(Json.parse("""{"$oid":"someId"}""")) must be(JsSuccess(IDType("someId"), JsPath \ "$oid"))
    }

    "write json successfully" in {

      IDType.write.writes(IDType("someId")) must be(Json.parse("""{"$oid":"someId"}"""))
    }
  }
}
