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

import forms.businessmatching.BusinessTypeFormProvider
import models.businessmatching.BusinessType.LimitedCompany
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.BusinessTypeView

class BusinessTypeViewSpec extends AmlsViewSpec with Matchers {

  lazy val business_type = inject[BusinessTypeView]
  lazy val fp            = inject[BusinessTypeFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture

  "BusinessTypeView" must {
    "have correct title, heading and subheading" in new ViewFixture {

      def view = business_type(fp().fill(LimitedCompany))

      doc.title       must startWith(
        messages("businessmatching.businessType.title") + " - " + messages("summary.businessmatching")
      )
      heading.html    must be(messages("businessmatching.businessType.title"))
      subHeading.html must include(messages("summary.businessmatching"))
    }

    behave like pageWithErrors(
      business_type(fp().withError("businessType", "businessmatching.businessType.error")),
      "businessType",
      "businessmatching.businessType.error"
    )

    behave like pageWithBackLink(business_type(fp()))
  }
}
