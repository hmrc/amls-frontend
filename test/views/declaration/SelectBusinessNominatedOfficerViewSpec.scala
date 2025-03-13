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
import forms.declaration.BusinessNominatedOfficerFormProvider
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.SelectBusinessNominatedOfficerView

class SelectBusinessNominatedOfficerViewSpec extends AmlsViewSpec with Matchers {

  lazy val officerView = inject[SelectBusinessNominatedOfficerView]
  lazy val fp          = inject[BusinessNominatedOfficerFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val people = Seq(
    ResponsiblePerson(PersonName("Test", None, "Person1").some),
    ResponsiblePerson(PersonName("Test", None, "Person2").some)
  )

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "SelectBusinessNominatedOfficerView" must {
    "have correct title, headings and content" in new ViewFixture {

      def view = officerView("subheading", fp(), Seq.empty[ResponsiblePerson])

      doc.title mustBe s"${messages("declaration.who.is.business.nominated.officer")} - ${messages("title.amls")} - ${messages("title.gov")}"
      heading.html    must be(messages("declaration.who.is.business.nominated.officer"))
      subHeading.html must include("subheading")
      doc.text()      must include(messages("declaration.who.is.business.nominated.officer.text"))
    }

    "have a list of responsible people" in new ViewFixture {

      def view = officerView("subheading", fp(), people)

      people.zipWithIndex.map { n =>
        n._1.personName.map { obj =>
          val id = s"value-${n._2}"
          val e  = doc.getElementById(id)

          Option(e) must be(defined)
          e.`val` mustBe s"${obj.firstName}${obj.lastName}"

          val label = doc.select(s"label[for=$id]")
          label.text() must include(obj.fullName)
        }
      }
    }

    "prepopulate the selected nominated officer" in new ViewFixture {

      def view = officerView("subheading", fp().fill(BusinessNominatedOfficer("TestPerson1")), people)

      doc.select("input[type=radio][id=value-0]").hasAttr("checked") mustBe true

    }

    "show the 'register someone else' radio button" in new ViewFixture {
      def view = officerView("subheading", fp(), Seq.empty[ResponsiblePerson])

      Option(doc.select("input[type=radio][id=other]")) must be(defined)
      doc.select("label[for=other]").text()             must include(messages("lbl.register.some.one.else"))
    }

    behave like pageWithErrors(
      officerView(
        "subheading",
        fp().withError("value", "error.required.declaration.nominated.officer"),
        Seq.empty[ResponsiblePerson]
      ),
      "value",
      "error.required.declaration.nominated.officer"
    )

    behave like pageWithBackLink(officerView("subheading", fp(), Seq.empty[ResponsiblePerson]))
  }
}
