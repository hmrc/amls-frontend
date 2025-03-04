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

import forms.businessactivities.AccountantForAMLSRegulationsFormProvider
import models.businessactivities.AccountantForAMLSRegulations
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.AccountantForAMLSRegulationsView

class AccountantForAMLSRegulationsViewSpec extends AmlsViewSpec with Matchers {

  lazy val accountant: AccountantForAMLSRegulationsView           = inject[AccountantForAMLSRegulationsView]
  lazy val formProvider: AccountantForAMLSRegulationsFormProvider = inject[AccountantForAMLSRegulationsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AccountantForAMLSRegulationsView view" must {
    "have correct title" in new ViewFixture {

      def view = accountant(formProvider().fill(AccountantForAMLSRegulations(true)), true)

      doc.title must startWith(
        messages("businessactivities.accountantForAMLSRegulations.title") + " - " + messages(
          "summary.businessactivities"
        )
      )
    }

    "have correct headings" in new ViewFixture {

      def view = accountant(formProvider().fill(AccountantForAMLSRegulations(false)), true)

      heading.html    must be(messages("businessactivities.accountantForAMLSRegulations.title"))
      subHeading.html must include(messages("summary.businessactivities"))
    }

    behave like pageWithErrors(
      accountant(
        formProvider().withError("accountantForAMLSRegulations", "error.required.ba.business.use.accountant"),
        true
      ),
      "accountantForAMLSRegulations",
      "error.required.ba.business.use.accountant"
    )

    behave like pageWithBackLink(accountant(formProvider(), false))
  }
}
