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

package views.businessactivities

import forms.businessactivities.EmployeeCountAMLSSupervisionFormProvider
import models.businessactivities.EmployeeCountAMLSSupervision
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.BusinessEmployeesAMLSSupervisionView

class BusinessEmployeesAMLSSupervisionViewSpec extends AmlsViewSpec with Matchers {

  lazy val business: BusinessEmployeesAMLSSupervisionView         = inject[BusinessEmployeesAMLSSupervisionView]
  lazy val formProvider: EmployeeCountAMLSSupervisionFormProvider = inject[EmployeeCountAMLSSupervisionFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "business_employees view" must {
    "have correct title" in new ViewFixture {

      def view = business(formProvider().fill(EmployeeCountAMLSSupervision("11")), true)

      doc.title must startWith(
        messages("businessactivities.employees.amls.supervision.title") + " - " + messages("summary.businessactivities")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = business(formProvider().fill(EmployeeCountAMLSSupervision("22")), true)

      heading.text()    must be(messages("businessactivities.employees.amls.supervision.title"))
      subHeading.text() must include(messages("summary.businessactivities"))
    }

    behave like pageWithErrors(
      business(
        formProvider().withError("employeeCountAMLSSupervision", "error.empty.ba.mlr.employee.count"),
        false
      ),
      "employeeCountAMLSSupervision",
      "error.empty.ba.mlr.employee.count"
    )

    behave like pageWithBackLink(business(formProvider(), true))
  }
}
