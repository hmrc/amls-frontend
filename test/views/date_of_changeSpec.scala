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

package views

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.html.date_of_change

class date_of_changeSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val date_of_change = app.injector.instanceOf[date_of_change]
    implicit val requestWithToken = addTokenForView()
  }

  "Date of Change View" must {

    val form2: ValidForm[DateOfChange] = Form2(DateOfChange(LocalDate.now()))

    "Have the correct title" in new ViewFixture {
      def view = date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      doc.title must startWith(Messages("dateofchange.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      heading.html must be (Messages("dateofchange.title"))
      subHeading.html must include ("testSubheadingMessage")
    }

    "contain the expected content elements" in new ViewFixture{
      def view = date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      html must include(Messages("lbl.date.example"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "dateOfChange") -> Seq(ValidationError("not a message Key"))
        ))

      def view = date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      errorSummary.html() must include("not a message Key")

      doc.getElementById("dateOfChange")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "have a back link" in new ViewFixture {
      def view = date_of_change(
        EmptyForm,
        "testSubheadingMessage",
        controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
      )

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}