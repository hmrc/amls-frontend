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

package views.renewal

import models.registrationprogress.TaskList
import models.status.{ReadyForRenewal, RenewalSubmitted}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.RenewalProgressView

import java.time.LocalDate

class RenewalProgressViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture

  lazy val renewal_progress                                      = inject[RenewalProgressView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  val renewalDate      = LocalDate.now().plusDays(15)
  val readyForRenewal  = ReadyForRenewal(Some(renewalDate))
  val renewalSubmitted = RenewalSubmitted(Some(renewalDate))
  val businessName     = "BusinessName"
  val serviceNames     = Seq("Service 1", "Service 2", "Service 3")

  "The renewal progress view" must {

    "Have the correct title and headings " in new ViewFixture {

      override def view = renewal_progress(TaskList(Seq.empty), businessName, serviceNames, true, true, readyForRenewal)

      doc.title must startWith(messages("renewal.progress.title"))

      doc.title    must be(
        messages("renewal.progress.title") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html must be(messages("renewal.progress.title"))
    }

    "show intro text" in new ViewFixture {

      override def view =
        renewal_progress(TaskList(Seq.empty), businessName, serviceNames, false, true, readyForRenewal)

      html must include(messages("renewal.progress.intro.1"))
      html must include(messages("renewal.progress.intro.2"))
    }

    "show the business name and services" in new ViewFixture {
      override def view =
        renewal_progress(TaskList(Seq.empty), businessName, serviceNames, false, true, readyForRenewal)

      val element = doc.getElementsByClass("govuk-summary-list__row").get(1)
      serviceNames.foreach(name =>
        element.text() must include {
          name
        }
      )
    }

    "not show the view details link under services section" in new ViewFixture {
      override def view =
        renewal_progress(TaskList(Seq.empty), businessName, serviceNames, false, true, readyForRenewal)

      val element = Option(doc.getElementById("view-details"))
      element mustBe None
    }

    "enable the submit registration button when can submit and renewal section complete" in new ViewFixture {
      override def view = renewal_progress(
        TaskList(Seq.empty),
        businessName,
        serviceNames,
        canSubmit = true,
        msbOrTcspExists = true,
        readyForRenewal,
        renewalSectionCompleted = true
      )

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe false

      html must include(messages("renewal.progress.ready.to.submit.intro"))

      doc.getElementsMatchingOwnText(messages("renewal.progress.edit")).attr("href") must be(
        controllers.renewal.routes.SummaryController.get.url
      )
    }

    "not have the submit registration button when cannot submit because renewal section not complete" in new ViewFixture {
      override def view = renewal_progress(
        TaskList(Seq.empty),
        businessName,
        serviceNames,
        canSubmit = false,
        msbOrTcspExists = true,
        readyForRenewal,
        renewalSectionCompleted = false
      )

      html must include(messages("renewal.progress.submit.intro"))

      doc.select(".application-submit form button[name=submit]").isEmpty mustBe true

      doc.getElementsMatchingOwnText(messages("renewal.progress.continue")).attr("href") must be(
        controllers.renewal.routes.WhatYouNeedController.get.url
      )
    }

    "not show the submit registration button when cannot submit and renewal section complete" in new ViewFixture {
      override def view = renewal_progress(
        TaskList(Seq.empty),
        businessName,
        serviceNames,
        canSubmit = false,
        msbOrTcspExists = true,
        readyForRenewal,
        renewalSectionCompleted = true
      )

      doc.select("form button[name=submit]").isEmpty mustBe true

      doc.getElementsMatchingOwnText(messages("renewal.progress.edit")).attr("href") must be(
        controllers.renewal.routes.SummaryController.get.url
      )
    }

    "show ready to submit renewal when information are completed" in new ViewFixture {

      override def view =
        renewal_progress(TaskList(Seq.empty), businessName, serviceNames, false, true, readyForRenewal, true)

      doc.select("#renewal-information-completed").get(0).text() must be(
        messages("renewal.progress.information.completed.info")
      )
    }

    "show submit renewal link and text when information are not completed yet" in new ViewFixture {

      override def view =
        renewal_progress(TaskList(Seq.empty), businessName, serviceNames, false, true, readyForRenewal, false)

      val space    = " "
      val fullStop = "."

      val expectedText = s"${messages("renewal.progress.information.not.completed.info.part1")}" +
        s"$space" +
        s"${messages("renewal.progress.information.not.completed.info.part2")}" +
        s"$fullStop"

      doc.select("#renewal-information-not-completed").get(0).text()  must be(expectedText)
      doc.select("#renewal-information-not-completed a").attr("href") must be(
        controllers.renewal.routes.WhatYouNeedController.get.url
      )
      html                                                            must include(messages("renewal.progress.submit.intro"))
    }

    "show the Nominated officer box with correct title, name and link" in new ViewFixture {

      val officerName = "FirstName LastName"

      def view = renewal_progress(
        TaskList(Seq.empty),
        businessName,
        serviceNames,
        false,
        true,
        readyForRenewal,
        false,
        hasCompleteNominatedOfficer = true,
        nominatedOfficerName = Some(officerName)
      )

      val element = doc.getElementsByClass("govuk-summary-list__row").get(1).text()

      element must include("Nominated officer")
      element must include(officerName)
    }

    "do not show the Nominated officer box if NO is not defined" in new ViewFixture {
      def view = renewal_progress(
        TaskList(Seq.empty),
        businessName,
        serviceNames,
        false,
        true,
        readyForRenewal,
        false,
        hasCompleteNominatedOfficer = false,
        nominatedOfficerName = None
      )

      val element = Option(doc.getElementById("nominated-officer"))

      element mustBe None
    }
  }
}
