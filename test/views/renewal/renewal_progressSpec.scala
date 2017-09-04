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

package views.renewal

import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.Strings.TextHelpers
import utils.{DateHelper, GenericTestHelper}
import views.Fixture

class renewal_progressSpec extends GenericTestHelper with MustMatchers{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val renewalDate = LocalDate.now().plusDays(15)

  }

  "The renewal progress view" must {

    "Have the correct title and headings " in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, true, true, Some(renewalDate))

      doc.title must startWith(Messages("renewal.progress.title"))

      doc.title must be(Messages("renewal.progress.title") +
        " - " + Messages("summary.status") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("renewal.progress.title"))
      subHeading.html must include(Messages("summary.status"))
    }

    "enable the submit registration button" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, true, true, Some(renewalDate))

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe false

      doc.select(".application-submit").get(0).text() must include(Messages("renewal.progress.submit.completed.intro"))
      doc.select(".application-submit").get(0).text() must include(Messages("renewal.progress.submit.completed.continue"))

      doc.getElementsMatchingOwnText(Messages("link.renewal.progress.change.answers")).attr("href") must be(controllers.renewal.routes.SummaryController.get().url)

    }

    "disable the submit registration button" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, false, true,Some(renewalDate))

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe true

      doc.select(".application-submit").get(0).text() must include(Messages("renewal.progress.submit.intro"))
      doc.getElementsMatchingOwnText(Messages("link.renewal.progress.change.answers")) must be(empty)
    }

    "show intro for MSB and TCSP businesses" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, false, true, Some(renewalDate))

      html must include (Messages("renewal.progress.tpandrp.intro", DateHelper.formatDate(renewalDate)).convertLineBreaks)
    }

    "show intro for non MSB and TCSP businesses" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(Seq.empty, false, false, Some(renewalDate))

      html must include (Messages("renewal.progress.tponly.intro", DateHelper.formatDate(renewalDate)).convertLineBreaks)
    }

  }

}