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

package views.status

import forms.EmptyForm
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{DateHelper, AmlsViewSpec}
import views.Fixture

class status_supervisedSpec extends AmlsViewSpec with MustMatchers {

  val activities = Set {
    "Money Service Business activities"
  }

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "status_supervised view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_supervised("XAML00000000000", Some("business Name"), Some(LocalDate.now), false, None, activities)

      doc.title must be(Messages("status.submissiondecisionsupervised.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecisionsupervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      def view = views.html.status.status_supervised("XAML00000000000", Some("business Name"), Some(LocalDate.now), false, None, activities)

      doc.getElementsByTag("h2").html() must include(Messages("status.hassomethingchanged"))
      doc.getElementsByTag("a").html() must include(Messages("status.amendment.edit.uppercase.start"))

      html must include (Messages("status.submissiondecisionsupervised.success.description"))
      doc.getElementsByClass("messaging").size() mustBe 2 // 1 for desktop, 1 for mobile

      val date = DateHelper.formatDate(LocalDate.now().plusDays(30))
      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionsupervised.enddate.text")).text must be
      Messages("status.submissiondecisionsupervised.enddate.text", date)

      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).attr("href") mustBe controllers.routes.NotificationController.getMessages().url

      html must include(controllers.changeofficer.routes.StillEmployedController.get.url)

      html must include("Your services")

      for (activity <- activities) {
        doc.getElementsByClass("list").html() must include("<li>" + activity + "</li>")
      }

      doc.getElementById("change-registered-services").attr("href") must be(controllers.businessmatching.updateservice.routes.ChangeBusinessTypesController.get().url)

    }

    "not contain the business activities change link if businessMatchingVariationToggle is false" in new ViewFixture {

      def view = views.html.status.status_supervised("XAML00000000000", Some("business Name"), Some(LocalDate.now), false, None, activities)
      doc.html mustNot include(controllers.businessmatching.routes.SummaryController.get().url)

    }


    "contain the expected content elements when status is ready for renewal" in new ViewFixture {
      def view = views.html.status.status_supervised("XAML00000000000", Some("business Name"), Some(LocalDate.now), true, None, activities)

      val renewalDate = LocalDate.now().plusDays(15)

      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionsupervised.enddate.text")).text must be
      Messages("status.submissiondecisionsupervised.enddate.text", renewalDate)

      for (activity <- activities) {
        doc.getElementsByClass("list").html() must include("<li>" + activity + "</li>")
      }

      doc.getElementById("change-registered-services").attr("href") mustBe
        controllers.businessmatching.updateservice.routes.ChangeBusinessTypesController.get().url

      doc.getElementsMatchingOwnText(Messages("status.readyforrenewal.warning")).text must be
      Messages("status.readyforrenewal.warning", renewalDate)
    }

    "contains expected survey link for supervised status" in new ViewFixture {
      def view = views.html.status.status_supervised("XAML00000000000", Some("business Name"), Some(LocalDate.now), false, None, activities)

      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.beforeyougo")).text() must
        be(Messages("survey.satisfaction.beforeyougo"))

      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.beforeyougo")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.beforeyougo")).attr("href") must be("/anti-money-laundering/satisfaction-survey")
//      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.answer")).hasAttr("href") must be(true)
//      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.answer")).attr("href") must be("/anti-money-laundering/satisfaction-survey")
    }

  }
}
