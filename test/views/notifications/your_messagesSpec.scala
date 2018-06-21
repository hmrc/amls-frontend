/*
 * Copyright 2018 HM Revenue & Customs
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

import models.notifications.NotificationRow
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.AmlsSpec
import views.Fixture

class your_messagesSpec extends AmlsSpec with MustMatchers  {

    trait ViewFixture extends Fixture {

        implicit val requestWithToken = addToken(request)

        val emptyNotifications: Seq[NotificationRow] = Seq()

        val businessName = "Fake Name Ltd."

        def view = views.html.notifications.your_messages(businessName, emptyNotifications, emptyNotifications)
    }

    "your_messages view" must {

        "have correct title" in new ViewFixture {
            doc.title must be(Messages("notifications.header") +
                    " - " + Messages("title.amls") +
                    " - " + Messages("title.gov"))
        }

        "have a back button link" in new ViewFixture {
            override def view: HtmlFormat.Appendable = ???
        }

        "have correct headings" in new ViewFixture {
            heading.html must be(Messages("notifications.header"))
        }

        "have a panel displaying the business name" in new ViewFixture {
            override def view: HtmlFormat.Appendable = ???
        }

        "have a first notification table title" in new ViewFixture {
            override def view: HtmlFormat.Appendable = ???
        }

        "have a first notification table header" in new ViewFixture {
            override def view: HtmlFormat.Appendable = ???
        }

        "have a row for each of the current notifications in the first notification table" in new ViewFixture {
            override def view: HtmlFormat.Appendable = ???
        }

        "for a row in a notification table" must {

            "display the subject of the notification" in new ViewFixture {
                override def view: HtmlFormat.Appendable = ???
            }

            "display the date of the notification" in new ViewFixture {
                override def view: HtmlFormat.Appendable = ???
            }

        }

        "when previous notifications is an empty Seq" must {

            "have one table with class notifications" in new ViewFixture {
                override def view: HtmlFormat.Appendable = ???
            }

        }

        "when previous notifications is a Seq containing notifications" must {

            "have a second notification table header" in new ViewFixture {
                override def view: HtmlFormat.Appendable = ???
            }

            "have two tables with class notifications" in new ViewFixture {
                override def view: HtmlFormat.Appendable = ???
            }

            "have a second notification table title" in new ViewFixture {
                override def view: HtmlFormat.Appendable = ???
            }

            "have a row for each of the previous notification" in new ViewFixture {
                override def view: HtmlFormat.Appendable = ???
            }

        }

    }

}
