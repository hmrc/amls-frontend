/*
 * Copyright 2019 HM Revenue & Customs
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

package views.changeofficer

import forms.{EmptyForm, InvalidForm}
import generators.ResponsiblePersonGenerator
import jto.validation.{Path, ValidationError}
import org.scalacheck.Gen
import org.scalatest.MustMatchers
import utils.AmlsSpec
import play.api.i18n.Messages
import views.Fixture


class new_nominated_officerSpec extends AmlsSpec with MustMatchers with ResponsiblePersonGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "new_nominated_officer view" must {
    "contain a list of responsible people" in new ViewFixture {

      val form2 = EmptyForm

      val responsiblePeople = Gen.listOf(responsiblePersonGen).sample.get


      def view = views.html.changeofficer.new_nominated_officer(EmptyForm, responsiblePeople)

      doc.title must be(Messages("changeofficer.newnominatedofficer.title") +
        " - " + Messages("summary.updateinformation") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))

      heading.html must be(Messages("changeofficer.newnominatedofficer.heading"))
      subHeading.html must include(Messages("summary.updateinformation"))

    }
    "have a back link" in new ViewFixture {
      def view = views.html.changeofficer.new_nominated_officer(EmptyForm, Nil)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.changeofficer.new_nominated_officer(form2, Nil)

      noException must be thrownBy doc.getElementById("id1")
      noException must be thrownBy doc.getElementById("id2")
      noException must be thrownBy doc.getElementById("id3")

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "test") -> Seq(ValidationError("not a message Key")),
          (Path \ "test2") -> Seq(ValidationError("second not a message Key")),
          (Path \ "test3") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.changeofficer.new_nominated_officer(form2, Nil)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")

    }
  }
}
