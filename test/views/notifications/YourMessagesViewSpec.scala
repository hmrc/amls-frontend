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

package views.notifications

import models.notifications.ContactType.RenewalApproval
import models.notifications.StatusType.{Approved, Rejected}
import models.notifications._
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import uk.gov.hmrc.govukfrontend.views.Aliases.Table
import utils.AmlsViewSpec
import views.Fixture
import views.html.notifications.YourMessagesView

import java.time.LocalDateTime

class YourMessagesViewSpec extends AmlsViewSpec with Matchers {

  lazy val your_messages                                         = app.injector.instanceOf[YourMessagesView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  val businessName            = "Fake Name Ltd."
  val headingWithBusinessName = "Messages for Fake Name Ltd."

  trait ViewFixture extends Fixture {

    val emptyNotifications: Seq[(NotificationRow, Int)] = Seq()
    val notifications: Seq[(NotificationRow, Int)]      =
      Seq(
        (
          NotificationRow(
            Some(Status(Some(Approved), None)),
            Some(RenewalApproval),
            Some("123456789"),
            false,
            LocalDateTime.of(2018, 4, 1, 0, 0),
            false,
            "XAML00000123456",
            "v1m0",
            IDType("123")
          ),
          0
        ),
        (
          NotificationRow(
            Some(Status(Some(Approved), None)),
            Some(RenewalApproval),
            Some("123456789"),
            false,
            LocalDateTime.of(2018, 3, 1, 0, 0),
            false,
            "XAML00000123456",
            "v1m0",
            IDType("123")
          ),
          1
        ),
        (
          NotificationRow(
            Some(Status(Some(Rejected), None)),
            Some(RenewalApproval),
            Some("123456789"),
            false,
            LocalDateTime.of(2018, 2, 1, 0, 0),
            false,
            "XAML00000123456",
            "v1m0",
            IDType("123")
          ),
          2
        )
      )

    val currentNotificationsTable = Table(
      notifications.map { case (row, index) =>
        row.asTableRows("current-application-notifications", index)
      }
    )

    val previousNotificationsTable = Table(
      notifications.map { case (row, index) =>
        row.asTableRows("previous-application-notifications", index)
      }
    )

    def view = your_messages(businessName, Table(), None)
  }

  trait CurrentNotificationsOnlyViewFixture extends ViewFixture {
    override def view = your_messages(businessName, currentNotificationsTable, None)
  }

  trait CurrentNotificationsAndPreviousNotificationsViewFixture extends ViewFixture {
    override def view = your_messages(businessName, currentNotificationsTable, Some(previousNotificationsTable))
  }

  "YourMessagesView" must {

    "have correct title" in new ViewFixture {
      doc.title must be(
        messages("notifications.header") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {
      heading.html must be(headingWithBusinessName)
    }

    behave like pageWithBackLink(your_messages(businessName, Table(), None))

    "have the correct caption" in new ViewFixture {
      doc.getElementById("your-registration").text() mustEqual messages("summary.status")
    }

    "have a first notification table" in new CurrentNotificationsOnlyViewFixture {
      Option(doc.getElementById("current-application-notifications")).isDefined mustEqual true
    }

    "have a row for each of the current notifications in the first notification table" in new CurrentNotificationsOnlyViewFixture {
      doc.getElementById("current-application-notifications").getElementsByTag("tr").size() mustEqual 3
    }

    "when current notifications is a Seq containing notifications" must {

      "for a row in a notification table" must {

        "display the subject of the notification" in new CurrentNotificationsOnlyViewFixture {
          val tableRows = doc.getElementById("current-application-notifications").getElementsByTag("tr")
          notifications.indices foreach { i =>
            tableRows.get(i).text must include(messages(notifications(i)._1.subject))
          }
        }

        "display the type of the notification" in new CurrentNotificationsOnlyViewFixture {
          val tableRows = doc.getElementById("current-application-notifications").getElementsByTag("tr")
          notifications.indices foreach { i =>
            tableRows.get(i).text must include(messages(notifications(i)._1.notificationType))
          }
        }

        "display the date of the notification" in new CurrentNotificationsOnlyViewFixture {
          val tableRows = doc.getElementById("current-application-notifications").getElementsByTag("tr")
          notifications.indices foreach { i =>
            tableRows.get(i).text must include(notifications(i)._1.dateReceived)
          }
        }
      }
    }

    "when previous notifications is an empty Seq" must {

      "have one table with class notifications" in new CurrentNotificationsOnlyViewFixture {
        doc.getElementsByTag("table").size() mustEqual 1
        Option(doc.getElementById("previous-application-notifications")).isDefined mustEqual false
      }

      "not have previous registration title" in new ViewFixture {
        Option(doc.getElementById("previous-reg-title")).isDefined mustEqual false
      }
    }

    "when previous notifications is a Seq containing notifications" must {

      "have a panel displaying previous registration title" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
        doc.getElementById("previous-reg-title").text mustEqual messages("notifications.previousReg")
      }

      "have two tables with class notifications" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
        doc.getElementsByTag("table").size() mustEqual 2
      }

      "have a second notification table" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
        Option(doc.getElementById("previous-application-notifications")).isDefined mustEqual true
      }

      "have a row for each of the previous notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
        doc.getElementById("previous-application-notifications").getElementsByTag("tr").size() mustEqual 3
      }

      "for a row in a notification table" must {

        "display the subject of the notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
          val tableRows = doc.getElementById("previous-application-notifications").getElementsByTag("tr")
          notifications.indices foreach { i =>
            tableRows.get(i).text must include(messages(notifications(i)._1.subject))
          }
        }

        "display the type of the notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
          val tableRows = doc.getElementById("previous-application-notifications").getElementsByTag("tr")
          notifications.indices foreach { i =>
            tableRows.get(i).text must include(messages(notifications(i)._1.notificationType))
          }
        }

        "display the date of the notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
          val tableRows = doc.getElementById("previous-application-notifications").getElementsByTag("tr")
          notifications.indices foreach { i =>
            tableRows.get(i).text must include(notifications(i)._1.dateReceived)
          }
        }

      }

    }

  }

}
