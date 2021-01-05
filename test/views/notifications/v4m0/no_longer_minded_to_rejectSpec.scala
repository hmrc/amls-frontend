/*
 * Copyright 2021 HM Revenue & Customs
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

package views.notifications.v4m0

import models.notifications.NotificationParams
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.notifications.v4m0.no_longer_minded_to_reject

class no_longer_minded_to_rejectSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val no_longer_minded_to_reject = app.injector.instanceOf[no_longer_minded_to_reject]
    implicit val requestWithToken = addTokenForView()

    val notificationParams = NotificationParams(safeId = Some("reference"))

  }

  "minded_to_reject view" must {

    "have correct title" in new ViewFixture {

      def view = no_longer_minded_to_reject(notificationParams)

      doc.title must be("No longer considering refusal" +
        " - " + "Your registration" +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = no_longer_minded_to_reject(notificationParams)

      heading.html must be("No longer considering refusal")
      subHeading.html must include("Your registration")

    }

    "have correct reference displayed" in new ViewFixture {

      def view = no_longer_minded_to_reject(notificationParams)

      doc.html must include("reference")
    }

    "have a back link" in new ViewFixture {

      def view = no_longer_minded_to_reject(notificationParams)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

  }

}
