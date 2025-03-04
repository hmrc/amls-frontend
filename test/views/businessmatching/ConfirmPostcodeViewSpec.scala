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

package views.businessmatching

import forms.businessmatching.ConfirmPostcodeFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.ConfirmPostcodeView

class ConfirmPostcodeViewSpec extends AmlsViewSpec with Matchers {

  lazy val confirm_postcode                                      = inject[ConfirmPostcodeView]
  lazy val fp                                                    = inject[ConfirmPostcodeFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "ConfirmPostcodeView" must {
    "have correct title" in new ViewFixture {

      def view = confirm_postcode(fp())

      doc.title                                must startWith(
        messages("businessmatching.confirm.postcode.title") + " - " + messages("summary.businessmatching")
      )
      heading.html                             must include(messages("businessmatching.confirm.postcode.title"))
      subHeading.html                          must include(messages("summary.businessmatching"))
      doc.select(s"input[id=postCode]").size() must be(1)
    }

    behave like pageWithErrors(
      confirm_postcode(fp().withError("postCode", "error.invalid.postcode")),
      "postCode",
      "error.invalid.postcode"
    )
  }
}
