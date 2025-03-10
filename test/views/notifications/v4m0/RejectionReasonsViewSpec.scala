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

package views.notifications.v4m0

import models.notifications.NotificationParams
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.notifications.v4m0.RejectionReasonsView

class RejectionReasonsViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val rejection_reasons                                     = app.injector.instanceOf[RejectionReasonsView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

    val notificationParams = NotificationParams(
      businessName = Some("Fake Name Ltd."),
      msgContent = "msgContent",
      safeId = Some("reference"),
      endDate = Some("endDate")
    )
  }

  "RejectionReasonsView" must {

    "have correct title" in new ViewFixture {

      def view = rejection_reasons(notificationParams)

      doc.title must be(
        "Your application has been refused" +
          " - " + "Your registration" +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = rejection_reasons(notificationParams)

      heading.html    must be("Your application has been refused")
      subHeading.html must include("Your registration")
    }

    "have correct content, businessName, endDate and reference displayed" in new ViewFixture {

      def view = rejection_reasons(notificationParams)

      doc.html must (include("msgContent") and include("Fake Name Ltd.") and include("reference") and include(
        "endDate"
      ))
    }

    "have a back link" in new ViewFixture {

      def view = rejection_reasons(notificationParams)

      doc.getElementById("return-to-messages").attr("href") mustBe controllers.routes.NotificationController
        .getMessages()
        .url
    }
  }
}
