/*
 * Copyright 2017 HM Revenue & Customs
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
import forms.{EmptyForm, Form2, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class register_partnersSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "register_partners view" must {
    "have correct title, headings and content" in new ViewFixture {

      def view = views.html.declaration.register_partners("subheading", EmptyForm, Seq.empty[ResponsiblePeople], Seq("partner1"))

      doc.title mustBe s"${Messages("declaration.register.partners.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.register.partners.title"))
      subHeading.html must include("subheading")
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "value") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.declaration.register_partners("subheading", form2, Seq(ResponsiblePeople()), Seq("partner1"))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("value")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "have a list of responsible people" in new ViewFixture {

      val people = Seq(
        ResponsiblePeople(PersonName("Test", None, "Person1").some),
        ResponsiblePeople(PersonName("Test", None, "Person2").some)
      )

      def view = views.html.declaration.register_partners("subheading", EmptyForm, people, Seq("partner1"))

      people map(_.personName.get) foreach { n =>
        val id = s"value-${n.fullNameWithoutSpace}"
        val e = doc.getElementById(id)

        Option(e) must be(defined)
        e.`val` mustBe s"${n.firstName}${n.lastName}"

        val label = doc.select(s"label[for=$id]")
        label.text() must include(n.fullName)
      }

      val id = s"value--1"
      val e = doc.getElementById(id)

      Option(e) must be(defined)
      e.`val` mustBe "-1"

      val label = doc.select(s"label[for=$id]")
      label.text() must include(Messages("lbl.register.some.one.else"))

    }

    "show the correct text when there are no current partners" in new ViewFixture {

      val people = Seq(
        ResponsiblePeople(PersonName("Test", None, "Person1").some),
        ResponsiblePeople(PersonName("Test", None, "Person2").some)
      )

      val currentPartners = Seq.empty

      def view = views.html.declaration.register_partners("subheading", EmptyForm, people, currentPartners)

      html must include(Messages("declaration.register.partners.none.text"))
    }

    "show the correct text when there is one current partner" in new ViewFixture {

      val people = Seq(
        ResponsiblePeople(PersonName("Test", None, "Person1").some),
        ResponsiblePeople(PersonName("Test", None, "Person2").some)
      )

      val currentPartners = Seq("firstName lastName")

      def view = views.html.declaration.register_partners("subheading", EmptyForm, people, currentPartners)

      html must include(Messages("declaration.register.partners.one.text", currentPartners.head))
    }


  }
}
