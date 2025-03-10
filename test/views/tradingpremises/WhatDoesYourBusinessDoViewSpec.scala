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

package views.tradingpremises

import forms.tradingpremises.WhatDoesYourBusinessDoFormProvider
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.WhatDoesYourBusinessDoView

class WhatDoesYourBusinessDoViewSpec extends AmlsViewSpec with Matchers {

  lazy val what_does_your_business_do = inject[WhatDoesYourBusinessDoView]
  lazy val fp                         = inject[WhatDoesYourBusinessDoFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhatDoesYourBusinessDoView" must {

    val formValues = BusinessMatchingActivities.formValues()

    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.whatdoesyourbusinessdo.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = what_does_your_business_do(fp(), formValues, false, 1)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.whatdoesyourbusinessdo.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.select("input[type=checkbox]").size mustEqual BusinessMatchingActivities.all.size
    }

    behave like pageWithErrors(
      what_does_your_business_do(
        fp().withError("value", "error.required.tp.activity.your.business.do"),
        formValues,
        true,
        1
      ),
      "value",
      "error.required.tp.activity.your.business.do"
    )

    behave like pageWithBackLink(what_does_your_business_do(fp(), formValues, false, 1))
  }
}
