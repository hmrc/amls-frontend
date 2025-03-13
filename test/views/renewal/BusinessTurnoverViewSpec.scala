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

import forms.renewal.BusinessTurnoverFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.BusinessTurnoverView

class BusinessTurnoverViewSpec extends AmlsViewSpec with Matchers {

  lazy val business_turnover                                     = inject[BusinessTurnoverView]
  lazy val fp                                                    = inject[BusinessTurnoverFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "BusinessTurnoverView" must {
    "have correct title" ignore new ViewFixture {

      def view = business_turnover(fp(), true)

      doc.title must startWith(messages("renewal.business-turnover.title") + " - " + messages("summary.renewal"))
    }

    "have correct headings" ignore new ViewFixture {

      def view = business_turnover(fp(), true)

      heading.html    must be(messages("renewal.business-turnover.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    behave like pageWithErrors(
      business_turnover(
        fp().withError("businessTurnover", "error.required.renewal.ba.business.turnover"),
        false
      ),
      "businessTurnover",
      "error.required.renewal.ba.business.turnover"
    )

    behave like pageWithBackLink(business_turnover(fp(), false))
  }
}
