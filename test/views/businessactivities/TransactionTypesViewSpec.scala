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

import forms.businessactivities.TransactionTypesFormProvider
import models.businessactivities.TransactionTypes
import models.businessactivities.TransactionTypes.{DigitalSoftware, Paper}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.html.businessactivities.TransactionTypesView

class TransactionTypesViewSpec extends AmlsViewSpec with Matchers {

  import views.Fixture

  lazy val transaction: TransactionTypesView          = inject[TransactionTypesView]
  lazy val formProvider: TransactionTypesFormProvider = inject[TransactionTypesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "transaction_types view" must {
    "have correct title" in new ViewFixture {

      def view = transaction(formProvider().fill(TransactionTypes(Set(Paper))), true)

      doc.title must startWith(messages("businessactivities.do.keep.records"))
    }

    "have correct headings" in new ViewFixture {

      def view = transaction(formProvider().fill(TransactionTypes(Set(DigitalSoftware("name")))), true)

      heading.html    must be(messages("businessactivities.do.keep.records"))
      subHeading.html must include(messages("summary.businessactivities"))
    }

    behave like pageWithErrors(
      transaction(formProvider().withError("software", "error.required.ba.software.package.name"), true),
      "software",
      "error.required.ba.software.package.name"
    )

    behave like pageWithBackLink(transaction(formProvider(), false))
  }
}
