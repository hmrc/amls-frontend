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

import forms.renewal.PercentageFormProvider
import models.renewal.PercentageOfCashPaymentOver15000.{Second, Third}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.PercentageView

class PercentageViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture

  lazy val percentage                                            = inject[PercentageView]
  lazy val fp                                                    = inject[PercentageFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  "PercentageView" must {
    "have correct title" in new ViewFixture {

      def view = percentage(fp().fill(Second), true)

      doc.title must startWith(messages("renewal.hvd.percentage.title") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      def view = percentage(fp().fill(Third), true)

      heading.html    must be(messages("renewal.hvd.percentage.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    behave like pageWithErrors(
      percentage(fp().withError("percentage", "error.required.renewal.hvd.percentage"), false),
      "percentage",
      "error.required.renewal.hvd.percentage"
    )

    behave like pageWithBackLink(percentage(fp(), false))
  }
}
