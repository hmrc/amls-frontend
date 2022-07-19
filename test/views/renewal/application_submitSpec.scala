/*
 * Copyright 2022 HM Revenue & Customs
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
import utils.{DateHelper, AmlsViewSpec}
import views.Fixture

class application_submitSpec extends AmlsViewSpec with MustMatchers{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()

    val renewalDate = LocalDate.now().plusDays(15)

    val readyForRenewal = ReadyForRenewal(Some(renewalDate))
    val renewalSubmitted = RenewalSubmitted(Some(renewalDate))
  }

  "The renewal progress view" must {

    "show ready to submit message" when {
      "renewal is complete and can submit" in new ViewFixture {

        override def view = views.html.renewal.application_submit(true, readyForRenewal, true)

        doc.html must include(Messages("renewal.progress.ready.to.submit.intro"))
      }
    }

    "show section must be completed message" when {
      "renewal is complete and sections are incomplete" in new ViewFixture {
        override def view = views.html.renewal.application_submit(false, readyForRenewal, true)

        doc.html must include(Messages("renewal.progress.complete.sections.incomplete"))
      }
    }

    "show have not completed your renewal message" when {
      "renewal section is not complete" in new ViewFixture {

        override def view = views.html.renewal.application_submit(false, readyForRenewal, false)

        doc.html must include(Messages("renewal.progress.submit.intro"))
      }
    }
  }
}