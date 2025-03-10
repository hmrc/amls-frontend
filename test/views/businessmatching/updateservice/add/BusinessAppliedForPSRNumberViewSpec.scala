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

package views.businessmatching.updateservice.add

import forms.businessmatching.PSRNumberFormProvider
import models.businessmatching.BusinessAppliedForPSRNumberYes
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add.BusinessAppliedForPSRNumberView

class BusinessAppliedForPSRNumberViewSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture
  lazy val numberView                                            = inject[BusinessAppliedForPSRNumberView]
  lazy val formProvider                                          = inject[PSRNumberFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  def view = numberView(formProvider(), edit = false)

  "BusinessAppliedForPSRNumberView" must {

    "have correct title" in new ViewFixture {

      val filledForm = formProvider().fill(BusinessAppliedForPSRNumberYes("1234"))

      override def view = numberView(filledForm, edit = false)

      doc.title       must startWith(
        messages("businessmatching.updateservice.psr.number.title") + " - " + messages("summary.updateservice")
      )
      heading.html    must be(messages("businessmatching.updateservice.psr.number.title"))
      subHeading.html must include(messages("summary.updateservice"))
    }

    "hide the return to progress link" in new ViewFixture {
      val filledForm = formProvider().fill(BusinessAppliedForPSRNumberYes("1234"))

      override def view = numberView(filledForm, edit = true)

      doc.body().text() must not include messages("link.return.registration.progress")
    }

    behave like pageWithErrors(
      numberView(formProvider().withError("appliedFor", "error.required.msb.psr.options"), edit = false),
      "appliedFor",
      "error.required.msb.psr.options"
    )

    behave like pageWithErrors(
      numberView(formProvider().withError("regNumber", "error.max.msb.psr.number.format"), edit = false),
      "regNumber",
      "error.max.msb.psr.number.format"
    )

    behave like pageWithBackLink(view)
  }
}
