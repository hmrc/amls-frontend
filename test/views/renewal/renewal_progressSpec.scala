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

package views.renewal

import models.status.{ReadyForRenewal, RenewalSubmitted}
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.Strings.TextHelpers
import utils.{DateHelper, AmlsSpec}
import views.Fixture

class renewal_progressSpec extends AmlsSpec with MustMatchers{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val renewalDate = LocalDate.now().plusDays(15)

    val readyForRenewal = ReadyForRenewal(Some(renewalDate))
    val renewalSubmitted = RenewalSubmitted(Some(renewalDate))
  }

  "The renewal progress view" must {

    "Have the correct title and headings " in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, true, true, readyForRenewal)

      doc.title must startWith(Messages("renewal.progress.title"))

      doc.title must be(Messages("renewal.progress.title") +
        " - " + Messages("summary.status") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("renewal.progress.title"))
      subHeading.html must include(Messages("summary.status"))
    }

    "enable the submit registration button when can submit and renewal section complete" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, canSubmit = true, msbOrTcspExists = true, readyForRenewal, renewalSectionCompleted = true)

      doc.select("form button[name=submit]").attr("disabled").toBoolean mustBe false

      doc.select(".application-submit").get(0).text() must include(Messages("renewal.progress.submit.intro"))

      doc.getElementsMatchingOwnText(Messages("renewal.progress.edit")).attr("href") must be(controllers.renewal.routes.SummaryController.get().url)
    }

    "not have the submit registration button when cannot submit because renewal section not complete" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, canSubmit = false, msbOrTcspExists = true, readyForRenewal, renewalSectionCompleted = false)

      doc.select(".application-submit form button[name=submit]").isEmpty mustBe true

      doc.getElementsMatchingOwnText(Messages("renewal.progress.continue")).attr("href") must be(controllers.renewal.routes.WhatYouNeedController.get().url)
    }

    "disable the submit registration button when cannot submit and renewal section complete" in new ViewFixture {
      override def view = views.html.renewal.renewal_progress(Seq.empty, canSubmit = false, msbOrTcspExists = true, readyForRenewal, renewalSectionCompleted = true)

      doc.select("form button[name=submit]").attr("disabled").toBoolean mustBe true

      doc.select(".application-submit").get(0).text() must include(Messages("renewal.progress.submit.intro"))

      doc.getElementsMatchingOwnText(Messages("renewal.progress.edit")).attr("href") must be(controllers.renewal.routes.SummaryController.get().url)
    }

    "show intro for MSB and TCSP businesses" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, false, true, readyForRenewal)

      html must include (Messages("renewal.progress.tpandrp.intro", DateHelper.formatDate(renewalDate)).convertLineBreaks)
    }

    "show intro for non MSB and TCSP businesses" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, false, false, readyForRenewal)

      html must include (Messages("renewal.progress.tponly.intro", DateHelper.formatDate(renewalDate)).convertLineBreaks)
    }

  }

}