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

package views.businessdetails

import forms.businessdetails.ActivityStartDateFormProvider
import models.businessdetails.ActivityStartDate
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.ActivityStartDateView

import java.time.LocalDate


class ActivityStartDateViewSpec extends AmlsViewSpec with Matchers {
  trait ViewFixture extends Fixture {
    lazy val date = inject[ActivityStartDateView]
    lazy val formProvider = inject[ActivityStartDateFormProvider]
    implicit val requestWithToken = addTokenForView()
  }

  "ActivityStartDateView" must {
    "have correct title" in new ViewFixture {

      val formWithData: Form[ActivityStartDate] = formProvider().fill(ActivityStartDate(LocalDate.now))

      def view = date(formWithData, true)

      doc.title must startWith(messages("businessdetails.activity.start.date.title") + " - " + messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val formWithData: Form[ActivityStartDate] = formProvider().fill(ActivityStartDate(LocalDate.now))

      def view = date(formWithData, true)

      heading.html must be(messages("businessdetails.activity.start.date.title"))
      subHeading.html must include(messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val formWithInvalidData: Form[ActivityStartDate] = formProvider().bind(
        Map(
          "value.day" -> "2",
          "value.month" -> "4",
          "value.year" -> "x",
        )
      )

      def view = date(formWithInvalidData, true)

      doc.getElementsByClass("govuk-error-summary").text() must include(messages("error.invalid.date.not.real"))

      doc.getElementById("startDate-error").text() must include(messages("error.invalid.date.not.real"))

    }

    "have a back link" in new ViewFixture {
      def view = date(formProvider(), true)

      assert(doc.getElementById("back-link").isInstanceOf[Element])
    }
  }
}