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

package views.notifications.v1

import models.notifications.NotificationParams
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class no_longer_minded_to_rejectSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {

    implicit val requestWithToken = addToken(request)

    val notificationParams = NotificationParams(msgContent = "msgContent", reference = Some("reference"))

  }

  "minded_to_reject view" must {

    "have correct title" in new ViewFixture {

      def view = views.html.notifications.v1.no_longer_minded_to_reject(notificationParams)

      doc.title must be("No longer considering refusal" +
        " - " + "Your registration" +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.notifications.v1.no_longer_minded_to_reject(notificationParams)

      heading.html must be("No longer considering refusal")
      subHeading.html must include("Your registration")

    }

  }

}