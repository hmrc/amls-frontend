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

package views.status

import forms.EmptyForm
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class status_revokedSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_revoked view" must {
    val pageTitleSuffix = " - Your registration - " +Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading and static content" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_revoked("XAML00000000000", Some("business Name"))

      doc.title must be(Messages("status.submissiondecisionrevoked.title") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecision.not.supervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements, new application button enabled" in new ViewFixture {
      def view =  views.html.status.status_revoked("XAML00000000000", Some("business Name"))

      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionrevoked.description")).text must be(
        Messages("status.submissiondecisionrevoked.description"))
      doc.getElementById("revoked.p2").text must be (Messages("status.submissiondecisionrevoked.description2"))

      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).attr("href") mustBe controllers.routes.NotificationController.getMessages().url

      doc.getElementById("new.application.button").html() must be (Messages("status.newsubmission.btn"))
      doc.getElementsByTag("form").attr("action") mustBe controllers.routes.StatusController.newSubmission().url
    }
  }
}