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
import views.html.notifications.v4m0.minded_to_revoke

class minded_to_revokeSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val minded_to_revoke = app.injector.instanceOf[minded_to_revoke]
    implicit val requestWithToken = addTokenForView()

    val notificationParams = NotificationParams(msgContent = "msgContent", businessName = Some("Fake Name Ltd."), amlsRefNo = Some("amlsRegNo"))

  }

  "minded_to_revoke view" must {

    "have correct title" in new ViewFixture {

      def view = minded_to_revoke(notificationParams)

      doc.title must be("Revocation being considered" +
        " - " + "Your registration" +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = minded_to_revoke(notificationParams)

      heading.html must be("Revocation being considered")
      subHeading.html must include("Your registration")

    }

    "have correct content, businessName and reference displayed" in new ViewFixture {

      def view = minded_to_revoke(notificationParams)

      doc.html must (include("msgContent") and include("Fake Name Ltd.") and include("amlsRegNo"))
    }


    "have a back link" in new ViewFixture {

      def view = minded_to_revoke(notificationParams)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

  }


}
