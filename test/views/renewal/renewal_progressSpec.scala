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

package views.renewal

import models.status.{ReadyForRenewal, RenewalSubmitted}
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.Strings.TextHelpers
import utils.{AmlsViewSpec, DateHelper}
import views.Fixture

class renewal_progressSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()

    val renewalDate = LocalDate.now().plusDays(15)
    val readyForRenewal = ReadyForRenewal(Some(renewalDate))
    val renewalSubmitted = RenewalSubmitted(Some(renewalDate))
    val businessName = "BusinessName"
    val serviceNames = Seq("Service 1", "Service 2", "Service 3")
  }

  "The renewal progress view" must {

    "Have the correct title and headings " in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, true, true, readyForRenewal)

      doc.title must startWith(Messages("renewal.progress.title"))

      doc.title must be(Messages("renewal.progress.title") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("renewal.progress.title"))
    }

    "show intro text" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, false, true, readyForRenewal)

      html must include(Messages("renewal.progress.intro", DateHelper.formatDate(renewalDate)).convertLineBreaks)
    }

    "show the business name and services" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, false, true, readyForRenewal)

      val element = doc.getElementsByClass("grid-layout")
      serviceNames.foreach(name => element.text() must include {
        name
      })
    }

    "not show the view details link under services section" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, false, true, readyForRenewal)

      val element = Option(doc.getElementById("view-details"))
      element mustBe None
    }


    "enable the submit registration button when can submit and renewal section complete" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, canSubmit = true,
        msbOrTcspExists = true, readyForRenewal, renewalSectionCompleted = true)

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe false

      html must include(Messages("renewal.progress.ready.to.submit.intro"))

      doc.getElementsMatchingOwnText(Messages("renewal.progress.edit")).attr("href") must be(controllers.renewal.routes.SummaryController.get().url)
    }

    "not have the submit registration button when cannot submit because renewal section not complete" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, canSubmit = false,
        msbOrTcspExists = true, readyForRenewal, renewalSectionCompleted = false)

      html must include(Messages("renewal.progress.submit.intro"))

      doc.select(".application-submit form button[name=submit]").isEmpty mustBe true

      doc.getElementsMatchingOwnText(Messages("renewal.progress.continue")).attr("href") must be(controllers.renewal.routes.WhatYouNeedController.get().url)
    }

    "not show the submit registration button when cannot submit and renewal section complete" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, canSubmit = false,
        msbOrTcspExists = true, readyForRenewal, renewalSectionCompleted = true)

      doc.select("form button[name=submit]").isEmpty mustBe true

      doc.getElementsMatchingOwnText(Messages("renewal.progress.edit")).attr("href") must be(controllers.renewal.routes.SummaryController.get().url)
    }

    "show ready to submit renewal when information are completed" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, false, true, readyForRenewal, true)

      doc.select("#renewal-information-completed").get(0).text() must be(Messages("renewal.progress.information.completed.info"))
    }

    "show submit renewal link and text when information are not completed yet" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, false, true, readyForRenewal, false)

      val space = " "
      val fullStop = "."

      val expectedText = s"${Messages("renewal.progress.information.not.completed.info.part1")}" +
        s"$space" +
        s"${Messages("renewal.progress.information.not.completed.info.part2")}" +
        s"$fullStop"

      doc.select("#renewal-information-not-completed").get(0).text() must be(expectedText)
      doc.select("#renewal-information-not-completed a").attr("href") must be(controllers.renewal.routes.WhatYouNeedController.get().url)
      html must include(Messages("renewal.progress.submit.intro"))
    }

    "show the Nominated officer box with correct title, name and link" in new ViewFixture {
      def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, false, true, readyForRenewal, false,
        hasCompleteNominatedOfficer = true,
        nominatedOfficerName = Some("FirstName LastName"))

      val element = doc.getElementById("nominated-officer")

      element.getElementsByClass("heading-small").text() must be("Nominated officer")
      element.html() must include("FirstName LastName")
    }

    "do not show the Nominated officer box if NO is not defined" in new ViewFixture {
      def view = views.html.renewal.renewal_progress(Seq.empty, businessName, serviceNames, false, true, readyForRenewal, false,
        hasCompleteNominatedOfficer = false,
        nominatedOfficerName = None)

      val element = Option(doc.getElementById("nominated-officer"))

      element mustBe None
    }
  }
}