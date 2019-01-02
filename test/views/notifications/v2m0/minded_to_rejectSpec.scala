/*
 * Copyright 2019 HM Revenue & Customs
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

import models.notifications._
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class minded_to_rejectSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {

    implicit val requestWithToken = addToken(request)

    val notificationParams = NotificationParams(msgContent = "msgContent", businessName = Some("Fake Name Ltd."), safeId = Some("reference"))

  }

  "minded_to_reject view" must {

    "have correct title" in new ViewFixture {

      def view = views.html.notifications.v1m0.minded_to_reject(notificationParams)

      doc.title must be("Refusal being considered" +
        " - " + "Your registration" +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.notifications.v1m0.minded_to_reject(notificationParams)

      heading.html must be("Refusal being considered")
      subHeading.html must include("Your registration")

    }

    "have correct content, businessName and reference displayed" in new ViewFixture {

      def view = views.html.notifications.v1m0.minded_to_reject(notificationParams)

      doc.html must (include("msgContent") and include("Fake Name Ltd.") and include("reference"))
    }
  }
}
