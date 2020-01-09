/*
 * Copyright 2020 HM Revenue & Customs
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

package views.notifications.v2m0

import models.notifications.NotificationParams
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture

class no_longer_minded_to_revokeSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {

    implicit val requestWithToken = addTokenForView()

    val notificationParams = NotificationParams(amlsRefNo = Some("amlsRegNo"))

  }

  "minded_to_revoke view" must {

    "have correct title" in new ViewFixture {

      def view = views.html.notifications.v2m0.no_longer_minded_to_revoke(notificationParams)

      doc.title must be("No longer considering revocation" +
        " - " + "Your registration" +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.notifications.v2m0.no_longer_minded_to_revoke(notificationParams)

      heading.html must be("No longer considering revocation")
      subHeading.html must include("Your registration")

    }

    "have correct reference displayed" in new ViewFixture {

      def view = views.html.notifications.v2m0.no_longer_minded_to_revoke(notificationParams)

      doc.html must include("amlsRegNo")
    }

    "have a back link" in new ViewFixture {

      def view = views.html.notifications.v2m0.no_longer_minded_to_revoke(notificationParams)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }


  }

}
