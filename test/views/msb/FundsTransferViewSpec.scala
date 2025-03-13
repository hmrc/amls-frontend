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

package views.msb

import forms.msb.FundsTransferFormProvider
import models.moneyservicebusiness.FundsTransfer
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.FundsTransferView

class FundsTransferViewSpec extends AmlsViewSpec with Matchers {

  lazy val funds_transfer = inject[FundsTransferView]
  lazy val fp             = inject[FundsTransferFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "FundsTransferView view" must {

    "have correct title" in new ViewFixture {

      def view = funds_transfer(fp().fill(FundsTransfer(true)), true)

      doc.title must be(
        messages("msb.fundstransfer.title") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = funds_transfer(fp().fill(FundsTransfer(true)), true)

      heading.html    must be(messages("msb.fundstransfer.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    behave like pageWithErrors(
      funds_transfer(
        fp().withError("transferWithoutFormalSystems", "error.required.msb.fundsTransfer"),
        false
      ),
      "transferWithoutFormalSystems",
      "error.required.msb.fundsTransfer"
    )

    behave like pageWithBackLink(funds_transfer(fp(), false))
  }
}
