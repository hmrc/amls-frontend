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

package views.notifications.v1m0

import models.notifications.NotificationParams
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture

class revocation_reasonsSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {

    implicit val requestWithToken = addTokenForView()

    val notificationParams = NotificationParams(businessName = Some("Fake Name Ltd."), msgContent = "msgContent", amlsRefNo = Some("amlsRegNo"), endDate = Some("endDate"))

  }

  "revocation_reasons view" must {

    "have correct title" in new ViewFixture {

      def view = views.html.notifications.v1m0.revocation_reasons(notificationParams)

      doc.title must be("Your supervision has been revoked" +
        " - " + "Your registration" +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.notifications.v1m0.revocation_reasons(notificationParams)

      heading.html must be("Your supervision has been revoked")
      subHeading.html must include("Your registration")

    }

    "have correct content, businessName, endDate and reference displayed" in new ViewFixture {

      def view = views.html.notifications.v1m0.revocation_reasons(notificationParams)

      doc.html must (include("msgContent") and include("Fake Name Ltd.") and include("amlsRegNo") and include("endDate"))
    }

    "have a back link" in new ViewFixture {

      def view = views.html.notifications.v1m0.revocation_reasons(notificationParams)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
