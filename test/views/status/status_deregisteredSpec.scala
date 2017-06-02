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

class status_deregisteredSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val deregistrationDate = LocalDate.now
    def view = views.html.status.status_deregistered(Some("business Name"), Some(deregistrationDate))
  }

  "status_revoked view" must {
    val pageTitleSuffix = s" - Your registration - ${Messages("title.amls")} - ${Messages("title.gov")}"

    "have correct title, heading and sub heading and static content" in new ViewFixture {

      val form2 = EmptyForm

      doc.title must be(Messages("status.submissionderegistered.title") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecision.not.supervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {
      doc.getElementsMatchingOwnText(Messages("status.submissionderegistered.description", DateHelper.formatDate(deregistrationDate))).text must be(
        Messages("status.submissionderegistered.description", DateHelper.formatDate(deregistrationDate)))
      doc.getElementsMatchingOwnText(Messages("status.submissionderegistered.description2")).text must be(
        Messages("status.submissionderegistered.description2"))

      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).attr("href") must be("/anti-money-laundering/your-registration/your-messages")

    }

  }
}
