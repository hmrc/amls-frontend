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

package views.renewal

import forms.renewal.AMPTurnoverFormProvider
import models.renewal.AMPTurnover.{Second, Third}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.AMPTurnoverView

class AMPTurnoverViewSpec extends AmlsViewSpec with Matchers {

  lazy val amp_turnover                                          = inject[AMPTurnoverView]
  lazy val fp                                                    = inject[AMPTurnoverFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "AMPTurnoverView" must {
    "have correct title" in new ViewFixture {

      def view = amp_turnover(fp().fill(Second), true)

      doc.title must startWith(
        "How much of your turnover for the last 12 months came from sales of art for €10,000 or more? - Extend your supervision - Manage your anti-money laundering supervision - GOV.UK"
      )
    }

    "have correct headings" in new ViewFixture {

      def view = amp_turnover(fp().fill(Third), true)

      heading.html    must be(
        "How much of your turnover for the last 12 months came from sales of art for €10,000 or more?"
      )
      subHeading.html must include(messages("summary.renewal"))
    }

    behave like pageWithErrors(
      amp_turnover(
        fp().withError("percentageExpectedTurnover", "error.required.renewal.amp.percentage"),
        false
      ),
      "percentageExpectedTurnover",
      "error.required.renewal.amp.percentage"
    )
  }
}
