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

package views

import forms.DateOfChangeFormProvider
import models.DateOfChange
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.html.DateOfChangeView

import java.time.LocalDate

class DateOfChangeViewSpec extends AmlsViewSpec with Matchers {

  lazy val date_of_change = inject[DateOfChangeView]
  lazy val fp             = inject[DateOfChangeFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "DateOfChangeView" must {

    "Have the correct title" in new ViewFixture {
      def view = date_of_change(
        fp().fill(DateOfChange(LocalDate.now())),
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      doc.title must startWith(messages("dateofchange.title"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = date_of_change(
        fp(),
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      heading.html    must be(messages("dateofchange.title"))
      subHeading.html must include("testSubheadingMessage")
    }

    "contain the expected content elements" in new ViewFixture {
      def view = date_of_change(
        fp(),
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      html must include(messages("lbl.date.example"))
    }

    behave like pageWithErrors(
      date_of_change(
        fp().withError("dateOfChange", "error.future.date"),
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      ),
      "dateOfChange",
      "error.future.date"
    )

    behave like pageWithBackLink(
      date_of_change(
        fp(),
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )
    )
  }
}
