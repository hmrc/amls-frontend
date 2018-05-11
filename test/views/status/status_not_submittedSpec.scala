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
import play.api.mvc.Call
import utils.AmlsSpec
import views.Fixture

class status_not_submittedSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val call = mock[Call]
  }

  "status_not_submitted view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_not_submitted("XAML00000000000", Some("business Name"), call)

      doc.title must be(Messages("status.submissionready.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissionready.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      def view = views.html.status.status_not_submitted("XAML00000000000", Some("business Name"), call)

      doc.getElementsContainingOwnText("business Name").hasText must be(true)
      doc.getElementsContainingOwnText(Messages("status.business")).hasText must be(true)

      doc.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      doc.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      doc.getElementsByClass("list").first().child(0).html() must include(Messages("status.complete"))
      doc.getElementsByClass("list").first().child(1).html() must include(Messages("status.notsubmitted"))
      doc.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))

      doc.getElementsByClass("declaration").first().child(0).html() must be(Messages("status.hassomethingchanged"))
      doc.getElementsByClass("status-list").first().child(0).hasClass("status-list--complete") must be(true)
      doc.getElementsByClass("status-list").first().child(1).hasClass("status-list--pending") must be(true)
      doc.getElementsByClass("status-list").first().child(2).hasClass("status-list--upcoming") must be(true)

      doc.getElementsMatchingOwnText(Messages("status.submissionready.description")).text() must be(Messages("status.submissionready.description"))

      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).attr("href") mustBe controllers.routes.NotificationController.getMessages().url
    }

    "contains expected content 'update/amend information'" in new ViewFixture {

      def view = views.html.status.status_not_submitted("XAML00000000000", Some("business Name"), call)

      doc.getElementsByClass("statusblock").first().html() must include(Messages("status.hassomethingchanged"))
      doc.getElementsByClass("statusblock").first().html() must include(Messages("status.submissionready.changelink1"))

      doc.html() must not include Messages("survey.satisfaction.beforeyougo")
    }

    "do not show business name when 'business name' is empty" in new ViewFixture {

      def view = views.html.status.status_not_submitted("XAML00000000000", None, call)

      doc.getElementsContainingOwnText(Messages("status.business")).isEmpty must be(true)


    }
  }
}