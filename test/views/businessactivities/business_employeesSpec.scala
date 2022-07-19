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

package views.businessactivities

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.HowManyEmployees
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture
import views.html.businessactivities.business_employees


class business_employeesSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val employees = app.injector.instanceOf[business_employees]
    implicit val requestWithToken = addTokenForView()
  }

  "business_employees view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[HowManyEmployees] = Form2(HowManyEmployees(Some("ECount"), Some("SCount")))

      def view = employees(form2, true)

      doc.title must startWith(Messages("businessactivities.employees.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[HowManyEmployees] = Form2(HowManyEmployees(Some("ECount"), Some("SCount")))

      def view = employees(form2, true)

      heading.text() must be(Messages("businessactivities.employees.title"))
      subHeading.text() must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "employeeCount") -> Seq(ValidationError("not a message Key"))
        ))

      def view = employees(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("employeeCount")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {
      def view = employees(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
