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

package utils

import models.notifications.ContactType.NoSubject
import models.notifications._
import org.scalatestplus.play.PlaySpec

import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDateTime}

class ContactTypeHelperSpec extends PlaySpec with AmlsSpec {

  "ContactTypeHelper " must {
    "return correct ContactType" when {

      "contact type is populated" in {
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

        ContactTypeHelper.getContactType(
          model.status,
          model.contactType,
          model.variation
        ) mustBe ContactType.MindedToRevoke
      }

      "auto rejected" in {
        val model = NotificationRow(
          Some(
            Status(
              Some(StatusType.Rejected),
              Some(RejectedReason.FailedToPayCharges)
            )
          ),
          None,
          None,
          false,
          LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC),
          false,
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff")
        )

        ContactTypeHelper.getContactType(
          model.status,
          model.contactType,
          model.variation
        ) mustBe ContactType.ApplicationAutorejectionForFailureToPay
      }

      "variation approved" in {
        val model = NotificationRow(
          Some(
            Status(
              Some(StatusType.Approved),
              None
            )
          ),
          None,
          None,
          true,
          LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC),
          false,
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff")
        )

        ContactTypeHelper.getContactType(
          model.status,
          model.contactType,
          model.variation
        ) mustBe ContactType.RegistrationVariationApproval
      }

      "DeRegistrationEffectiveDateChange" in {
        val model = NotificationRow(
          Some(
            Status(
              Some(StatusType.DeRegistered),
              None
            )
          ),
          None,
          None,
          true,
          LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC),
          false,
          "XJML00000200000",
          "1",
          new IDType("5832e38e01000001005ca3ff")
        )

        ContactTypeHelper.getContactType(
          model.status,
          model.contactType,
          model.variation
        ) mustBe ContactType.DeRegistrationEffectiveDateChange
      }

      "return NoSubject when ContactType not determined" in {
        val model = NotificationRow(
          status = Some(
            Status(
              Some(StatusType.Approved),
              None
            )
          ),
          contactType = None,
          contactNumber = None,
          variation = false,
          receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC),
          isRead = false,
          amlsRegistrationNumber = "XJML00000200000",
          templatePackageVersion = "v1m0",
          _id = new IDType("5832e38e01000001005ca3ff")
        )

        ContactTypeHelper.getContactType(model.status, model.contactType, model.variation) mustBe NoSubject
        model.subject mustBe "notifications.subject.NoSubject"

      }
    }
  }
}
