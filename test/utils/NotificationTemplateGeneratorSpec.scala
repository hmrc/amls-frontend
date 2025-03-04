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

import matchers.table.TableMatchers._
import models.notifications.ContactType.{MindedToReject, ReminderToPayForManualCharges, RenewalReminder}
import models.notifications.StatusType.Rejected
import models.notifications.{IDType, NotificationDetails, NotificationRow, Status}
import models.status.SubmissionDecisionRejected
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.HeadCell
import views.notifications._

import java.time.LocalDateTime.now

class NotificationTemplateGeneratorSpec extends AmlsViewSpec {

  val versionOneMsg: V1M0   = app.injector.instanceOf[V1M0]
  val versionTwoMsg: V2M0   = app.injector.instanceOf[V2M0]
  val versionThreeMsg: V3M0 = app.injector.instanceOf[V3M0]
  val versionFourMsg: V4M0  = app.injector.instanceOf[V4M0]
  val versionFiveMsg: V5M0  = app.injector.instanceOf[V5M0]
  val versionSixMsg: V6M0   = app.injector.instanceOf[V6M0]

  val templateGenerator = new NotificationTemplateGenerator(
    versionOneMsg,
    versionTwoMsg,
    versionThreeMsg,
    versionFourMsg,
    versionFiveMsg,
    versionSixMsg,
    commonDependencies
  )

  "known template versions to versioned views" must {
    "not contain more than versioned view per template version" in {
      templateGenerator.templateMsgVersionsToVersionedViews
        .groupMap(_._2)(_._1)
        .filterNot(_._2.sizeIs == 1) must have size 0
    }
  }

  ".contactTypeToView" must {
    "throw runtime exception" when {
      "when no versioned view exists for a given template version" in {
        the[RuntimeException] thrownBy templateGenerator
          .contactTypeToView(
            MindedToReject,
            ("XDML00000567890", "safeId"),
            "business name",
            mindedToRejectNotificationDetails,
            SubmissionDecisionRejected,
            "T1000"
          )(FakeRequest()) must have message "Notification version T1000 not found"
      }
    }

    // todo 40+ tests for the template versions & the rendered views, just check the titles?
  }

  ".toTable" must {
    "return populated notifications table with correct table header ids" when {
      "notification rows are provided & is not previous registration" in {
        val table = templateGenerator
          .toTable(
            Seq(
              (mindedToRejectNotificationRow, 0),
              (payForManualChargeNotificationRow, 1),
              (renewalReminderNotificationRow, 2)
            ),
            "id",
            isPrevRegistration = false
          )

        table must haveNumRows(3)
        table must haveTableHeaders(expectedNotificationHeadersWithIDSuffix("currentMessages"))
      }

      "notification rows are provided & is previous registration" in {
        val table = templateGenerator
          .toTable(
            Seq(
              (mindedToRejectNotificationRow, 0),
              (payForManualChargeNotificationRow, 1),
              (renewalReminderNotificationRow, 2)
            ),
            "id",
            isPrevRegistration = true
          )

        table must haveNumRows(3)
        table must haveTableHeaders(expectedNotificationHeadersWithIDSuffix("previousMessages"))
      }
    }

    "return empty table" when {
      "no notification rows are received" in {
        val table = templateGenerator.toTable(Seq.empty[(NotificationRow, Int)], "id", false)
        table must haveNoRows
        table must haveNoTableHeaders
      }
    }
  }

  private val mindedToRejectNotificationDetails =
    NotificationDetails(Some(MindedToReject), Some(Status(Some(Rejected), None)), None, false, now)

  private val mindedToRejectNotificationRow =
    NotificationRow(
      Some(Status(Some(Rejected), None)),
      Some(MindedToReject),
      None,
      false,
      now,
      false,
      "XDML00000567890",
      "v6m0",
      IDType("id")
    )

  private val payForManualChargeNotificationRow =
    NotificationRow(
      Some(Status(Some(Rejected), None)),
      Some(ReminderToPayForManualCharges),
      None,
      false,
      now,
      false,
      "XDML00000567890",
      "v6m0",
      IDType("id")
    )

  private val renewalReminderNotificationRow =
    NotificationRow(
      Some(Status(Some(Rejected), None)),
      Some(RenewalReminder),
      None,
      false,
      now,
      false,
      "XDML00000567890",
      "v6m0",
      IDType("id")
    )

  private def expectedNotificationHeadersWithIDSuffix(suffix: String): Seq[HeadCell] =
    Seq(
      HeadCell(content = Text("Subject"), attributes = Map("id" -> s"subject-$suffix")),
      HeadCell(content = Text("Category"), attributes = Map("id" -> s"category-$suffix")),
      HeadCell(content = Text("Date"), attributes = Map("id" -> s"date-$suffix"))
    )

}
