/*
 * Copyright 2023 HM Revenue & Customs
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
import org.joda.time.DateTime
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.notifications.your_messages

class your_messagesSpec extends AmlsViewSpec with MustMatchers  {

    trait ViewFixture extends Fixture {
        lazy val your_messages = app.injector.instanceOf[your_messages]
        implicit val requestWithToken = addTokenForView()

        val emptyNotifications: Seq[(NotificationRow, Int)] = Seq()
        val notifications: Seq[(NotificationRow, Int)] = Seq(
            (NotificationRow(
                Some(Status(Some(Approved), None)),
                Some(RenewalApproval),
                Some("123456789"),
                false,
                new DateTime(2018, 4, 1, 0, 0),
                false,
                "XAML00000123456",
                "v1m0",
                IDType("123")
            ), 0),
            (NotificationRow(
                Some(Status(Some(Approved), None)),
                Some(RenewalApproval),
                Some("123456789"),
                false,
                new DateTime(2018, 3, 1, 0, 0),
                false,
                "XAML00000123456",
                "v1m0",
                IDType("123")
            ), 1),
            (NotificationRow(
                Some(Status(Some(Rejected), None)),
                Some(RenewalApproval),
                Some("123456789"),
                false,
                new DateTime(2018, 2, 1, 0, 0),
                false,
                "XAML00000123456",
                "v1m0",
                IDType("123")
            ), 2)
        )

        val businessName = "Fake Name Ltd."

        def view = your_messages(businessName, emptyNotifications, emptyNotifications)
    }

    trait CurrentNotificationsOnlyViewFixture extends ViewFixture {
        override def view = your_messages(businessName, notifications, emptyNotifications)
    }

    trait CurrentNotificationsAndPreviousNotificationsViewFixture extends ViewFixture {
        override def view = your_messages(businessName, notifications, notifications)
    }

    "your_messages view" must {

        "have correct title" in new ViewFixture {
            doc.title must be(Messages("notifications.header") +
              " - " + Messages("title.amls") +
              " - " + Messages("title.gov"))
        }

        "have a back button link" in new ViewFixture {
            Option(doc.getElementById("back-link")).isDefined mustBe(true)
        }

        "have correct headings" in new ViewFixture {
            heading.html must be(Messages("notifications.header"))
        }

        "have a back link" in new ViewFixture {
            doc.getElementsByAttributeValue("class", "link-back") must not be empty
        }

        "have a panel displaying the business name" in new ViewFixture {
            doc.getElementById("business-name").text mustEqual businessName
        }

        "have a first notification table" in new ViewFixture {
            Option(doc.getElementById("current-application-notifications")).isDefined mustEqual true
        }

        "have a row for each of the current notifications in the first notification table" in new CurrentNotificationsOnlyViewFixture {
            doc.getElementById("current-application-notifications").getElementsByClass("message-inbox__list-item").size() mustEqual 3
        }

        "when current notifications is a Seq containing notifications" must {

            "for a row in a notification table" must {

                "display the subject of the notification" in new CurrentNotificationsOnlyViewFixture {
                    val tableRows = doc.getElementById("current-application-notifications").getElementsByClass("message-inbox__list-item")
                    notifications.indices foreach { i =>
                        tableRows.get(i).text must include(Messages(notifications(i)._1.subject))
                    }
                }

                "display the type of the notification" in new CurrentNotificationsOnlyViewFixture {
                    val tableRows = doc.getElementById("current-application-notifications").getElementsByClass("message-inbox__list-item")
                    notifications.indices foreach { i =>
                        tableRows.get(i).text must include(Messages(notifications(i)._1.notificationType))
                    }
                }

                "display the date of the notification" in new CurrentNotificationsOnlyViewFixture {
                    val tableRows = doc.getElementById("current-application-notifications").getElementsByClass("message-inbox__list-item")
                    notifications.indices foreach { i =>
                        tableRows.get(i).text must include(notifications(i)._1.dateReceived)
                    }
                }
            }
        }

        "when previous notifications is an empty Seq" must {

            "have one table with class notifications" in new CurrentNotificationsOnlyViewFixture {
                doc.getElementsByClass("message-inbox__list").size() mustEqual 1
                Option(doc.getElementById("previous-application-notifications")).isDefined mustEqual false
            }

            "not have previous registration title" in new ViewFixture {
                Option(doc.getElementById("previous-reg-title")).isDefined mustEqual false
            }
        }

        "when previous notifications is a Seq containing notifications" must {

            "have a panel displaying previous registration title" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
                doc.getElementById("previous-reg-title").text mustEqual Messages("notifications.previousReg")
            }

            "have two tables with class notifications" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
                doc.getElementsByClass("message-inbox__list").size() mustEqual 2
            }

            "have a second notification table" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
                Option(doc.getElementById("previous-application-notifications")).isDefined mustEqual true
            }

            "have a row for each of the previous notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
                doc.getElementById("previous-application-notifications").getElementsByClass("message-inbox__list-item").size() mustEqual 3
            }

            "for a row in a notification table" must {

                "display the subject of the notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
                    val tableRows = doc.getElementById("previous-application-notifications").getElementsByClass("message-inbox__list-item")
                    notifications.indices foreach { i =>
                        tableRows.get(i).text must include(Messages(notifications(i)._1.subject))
                    }
                }

                "display the type of the notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
                    val tableRows = doc.getElementById("previous-application-notifications").getElementsByClass("message-inbox__list-item")
                    notifications.indices foreach { i =>
                        tableRows.get(i).text must include(Messages(notifications(i)._1.notificationType))
                    }
                }

                "display the date of the notification" in new CurrentNotificationsAndPreviousNotificationsViewFixture {
                    val tableRows = doc.getElementById("previous-application-notifications").getElementsByClass("message-inbox__list-item")
                    notifications.indices foreach { i =>
                        tableRows.get(i).text must include(notifications(i)._1.dateReceived)
                    }
                }

            }

        }

    }

}