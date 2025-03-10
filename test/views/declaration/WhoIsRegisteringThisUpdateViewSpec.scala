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

package views.declaration

import forms.declaration.WhoIsRegisteringFormProvider
import generators.ResponsiblePersonGenerator
import models.declaration.WhoIsRegistering
import models.responsiblepeople.ResponsiblePerson
import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.WhoIsRegisteringThisUpdateView

class WhoIsRegisteringThisUpdateViewSpec extends AmlsViewSpec with Matchers with ResponsiblePersonGenerator {

  lazy val updateView = inject[WhoIsRegisteringThisUpdateView]
  lazy val fp         = inject[WhoIsRegisteringFormProvider]
  lazy val updateForm = fp("update")

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhoIsRegisteringThisUpdateView" must {
    "have correct title, heading and required fields" in new ViewFixture {

      val people = Gen.listOfN(2, responsiblePersonGen).sample.get

      def view = updateView(updateForm.fill(WhoIsRegistering("PersonName")), people)

      doc.title mustBe s"${messages("declaration.who.is.registering.amendment.title")} - ${messages("title.amls")} - ${messages("title.gov")}"
      heading.html    must be(messages("declaration.who.is.registering.amendment.title"))
      subHeading.html must include(messages("submit.amendment.application"))

      people.zipWithIndex.foreach { case (p, i) =>
        val id = s"person-$i"
        doc.getElementById(id).`val`()     must be(i.toString)
        doc.getElementById(id).parent.text must include(p.personName.get.fullName)
      }

      doc.select("input[type=radio]").size mustBe people.size + 1
      doc.getElementsContainingOwnText(messages("declaration.who.is.registering.text")).hasText must be(true)
    }

    behave like pageWithErrors(
      updateView(
        updateForm.withError("person", "error.required.declaration.who.is.declaring.this.update"),
        Seq(ResponsiblePerson())
      ),
      "person",
      "error.required.declaration.who.is.declaring.this.update"
    )

    behave like pageWithBackLink(updateView(updateForm, Seq(ResponsiblePerson())))
  }
}
