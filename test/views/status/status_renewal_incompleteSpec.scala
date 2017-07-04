/*
 * Copyright 2017 HM Revenue & Customs
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
import utils.{DateHelper, GenericTestHelper}
import views.Fixture

class status_renewal_incompleteSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_incomplete view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_renewal_incomplete("XAML00000567890", Some("business Name"), None)

      doc.title must be(Messages("status.submissiondecisionsupervised.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecisionsupervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      val endDate = new LocalDate(2017,1,1)
      val endDateFormatted = DateHelper.formatDate(endDate)

      def view = views.html.status.status_renewal_incomplete("XAML00000567890", Some("business Name"), Some(endDate))

      doc.getElementsContainingOwnText("business Name").hasText must be(true)
      doc.getElementsContainingOwnText(Messages("status.business")).hasText must be(true)
      doc.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      doc.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      doc.getElementsByClass("list").first().child(0).html() must include(Messages("status.incomplete"))
      doc.getElementsByClass("list").first().child(0).attr("class") must be("status-list--pending status-list--start")
      doc.getElementsByClass("list").first().child(1).html() must include(Messages("status.submitted"))
      doc.getElementsByClass("list").first().child(1).attr("class") must be("status-list--upcoming")
      doc.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))
      doc.getElementsByClass("list").first().child(2).attr("class") must be("status-list--upcoming")

      doc.getElementsMatchingOwnText(Messages("status.renewalincomplete.description")).text must be(Messages("status.renewalincomplete.description"))
      doc.getElementsMatchingOwnText(Messages("status.renewalincomplete.description2", endDateFormatted)).text must be(Messages("status.renewalincomplete.description2", endDateFormatted))

      html must include(controllers.changeofficer.routes.StillEmployedController.get.url)

    }

    "not contain the link to change the nominated officer" in new ViewFixture {

      val endDate = new LocalDate(2017,1,1)
      def view = views.html.status.status_renewal_incomplete("XAML00000567890", Some("business Name"), Some(endDate), false)

      html must not include controllers.changeofficer.routes.StillEmployedController.get.url
    }
  }
}