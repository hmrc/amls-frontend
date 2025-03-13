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

import cats.implicits._
import forms.declaration.BusinessPartnersFormProvider
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.RegisterPartnersView

class RegisterPartnersViewSpec extends AmlsViewSpec with Matchers {

  lazy val partnersView: RegisterPartnersView = inject[RegisterPartnersView]
  lazy val fp: BusinessPartnersFormProvider   = inject[BusinessPartnersFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "RegisterPartnersView view" must {
    "have correct title, headings and content" in new ViewFixture {

      def view = partnersView("subheading", fp(), Seq.empty[ResponsiblePerson], Seq("partner1"))

      doc.title mustBe s"${messages("declaration.register.partners.title")} - ${messages("title.amls")} - ${messages("title.gov")}"
      heading.html    must include(messages("declaration.register.partners.title"))
      subHeading.html must include("subheading")
    }

    "have a list of responsible people" in new ViewFixture {

      val people = Seq(
        ResponsiblePerson(PersonName("Test", None, "Person1").some),
        ResponsiblePerson(PersonName("Test", None, "Person2").some)
      )

      def view = partnersView("subheading", fp(), people, Seq("partner1"))

      people map (_.personName.get) foreach { n =>
        val id = s"value-${n.fullNameWithoutSpace}"
        val e  = doc.getElementById(id)

        Option(e) must be(defined)
        e.`val` mustBe s"${n.firstName}${n.lastName}"

        val label = doc.select(s"label[for=$id]")
        label.text() must include(n.fullName)
      }

      val id = s"other"
      val e  = doc.getElementById(id)

      Option(e) must be(defined)
      e.`val` mustBe "-1"

      val label = doc.select(s"label[for=$id]")
      label.text() must include(messages("lbl.register.some.one.else"))
    }

    "show the correct text when there are no current partners" in new ViewFixture {

      val people = Seq(
        ResponsiblePerson(PersonName("Test", None, "Person1").some),
        ResponsiblePerson(PersonName("Test", None, "Person2").some)
      )

      val currentPartners = Seq.empty

      def view = partnersView("subheading", fp(), people, currentPartners)

      html must include(messages("declaration.register.partners.none.text"))
    }

    "show the correct text when there is one current partner" in new ViewFixture {

      val people = Seq(
        ResponsiblePerson(PersonName("Test", None, "Person1").some),
        ResponsiblePerson(PersonName("Test", None, "Person2").some)
      )

      val currentPartners = Seq("firstName lastName")

      def view = partnersView("subheading", fp(), people, currentPartners)

      html must include(messages("declaration.register.partners.one.text", currentPartners.head))
    }

    behave like pageWithErrors(
      partnersView(
        "subheading",
        fp().withError("value", "error.required.declaration.partners"),
        Seq.empty[ResponsiblePerson],
        Seq("partner1")
      ),
      "value",
      "error.required.declaration.partners"
    )

    behave like pageWithBackLink(partnersView("subheading", fp(), Seq.empty[ResponsiblePerson], Seq("partner1")))
  }
}
