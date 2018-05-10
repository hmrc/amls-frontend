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
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{DateHelper, AmlsSpec}
import views.Fixture

class status_renewal_not_submittedSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_incomplete view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_renewal_not_submitted("XAML00000567890", Some("business Name"), None, None)

      doc.title must be(Messages("status.submissiondecisionsupervised.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecisionsupervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      val endDate = new LocalDate(2017,1,1)
      val endDateFormatted = DateHelper.formatDate(endDate)

      def view = views.html.status.status_renewal_not_submitted("XAML00000567890", Some("business Name"), Some(endDate), None)

      doc.getElementsContainingOwnText("business Name").hasText must be(true)
      doc.getElementsContainingOwnText(Messages("status.business")).hasText must be(true)
      doc.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      doc.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      doc.getElementsByClass("list").first().child(0).html() must include(Messages("status.complete"))
      doc.getElementsByClass("list").first().child(0).attr("class") must be("status-list--complete")
      doc.getElementsByClass("list").first().child(1).html() must include(Messages("status.submitted"))
      doc.getElementsByClass("list").first().child(1).attr("class") must be("status-list--pending")
      doc.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))
      doc.getElementsByClass("list").first().child(2).attr("class") must be("status-list--upcoming")

      doc.getElementsMatchingOwnText(Messages("status.renewalnotsubmitted.description")).text must be(Messages("status.renewalnotsubmitted.description"))
      doc.getElementsMatchingOwnText(Messages("status.renewalnotsubmitted.description2")).text must be(Messages("status.renewalnotsubmitted.description2"))
      doc.getElementsMatchingOwnText(Messages("status.renewalnotsubmitted.description3", endDateFormatted)).text must be(Messages("status.renewalnotsubmitted.description3", endDateFormatted))
    }
  }
}