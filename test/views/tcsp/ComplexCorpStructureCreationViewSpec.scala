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

package views.tcsp

import forms.tcsp.ComplexCorpStructureCreationFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.ComplexCorpStructureCreationView

class ComplexCorpStructureCreationViewSpec extends AmlsViewSpec with Matchers {

  lazy val complex_corp_structure_creation = inject[ComplexCorpStructureCreationView]
  lazy val fp                              = inject[ComplexCorpStructureCreationFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ComplexCorpStructureCreationView view" must {
    "have correct title, heading amd subheading" in new ViewFixture {

      def view = complex_corp_structure_creation(fp(), true)

      val title = messages("tcsp.create.complex.corporate.structures.lbl") + " - " + messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      doc.title       must be(title)
      heading.html    must be(messages("tcsp.create.complex.corporate.structures.lbl"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    behave like pageWithErrors(
      complex_corp_structure_creation(
        fp().withError("complexCorpStructureCreation", "error.required.tcsp.complex.corporate.structures"),
        true
      ),
      "complexCorpStructureCreation",
      "error.required.tcsp.complex.corporate.structures"
    )

    behave like pageWithBackLink(complex_corp_structure_creation(fp(), true))
  }
}
