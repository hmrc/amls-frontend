/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.{Form2, InvalidForm, ValidForm}
import generators.ResponsiblePersonGenerator
import jto.validation.{Path, ValidationError}
import models.declaration.WhoIsRegistering
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalacheck.Gen
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.who_is_registering_this_update


class who_is_registering_this_updateSpec extends AmlsViewSpec with MustMatchers with ResponsiblePersonGenerator {

  trait ViewFixture extends Fixture {
    lazy val who_is_registering_this_update = app.injector.instanceOf[who_is_registering_this_update]
    implicit val requestWithToken = addTokenForView()
  }

  "who_is_registering_this_update view" must {
    "have correct title, heading and required fields" in new ViewFixture {
      val form2: ValidForm[WhoIsRegistering] = Form2(WhoIsRegistering("PersonName"))
      val people = Gen.listOfN(2, responsiblePersonGen).sample.get

      def view = who_is_registering_this_update(form2, people)

      doc.title mustBe s"${Messages("declaration.who.is.registering.amendment.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.who.is.registering.amendment.title"))
      subHeading.html must include(Messages("submit.amendment.application"))
      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      people.zipWithIndex.foreach { case (p, i) =>
        val id = s"person-$i"
        doc.getElementById(id).`val`() must be(i.toString)
        doc.getElementById(id).parent.text must include(p.personName.get.fullName)
      }

      doc.select("input[type=radio]").size mustBe people.size + 1
      doc.getElementsContainingOwnText(Messages("declaration.who.is.registering.text")).hasText must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "person") -> Seq(ValidationError("not a message Key"))
        ))

      def view = who_is_registering_this_update(form2, Seq(ResponsiblePerson()))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("person")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
