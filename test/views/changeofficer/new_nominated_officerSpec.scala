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

package views.changeofficer

import forms.EmptyForm
import generators.ResponsiblePersonGenerator
import org.scalacheck.Gen
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture


class new_nominated_officerSpec extends GenericTestHelper with MustMatchers with ResponsiblePersonGenerator {

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


//    "have correct form fields" in new ViewFixture {
//
//      val form2 = EmptyForm
//
//      def view = views.html.changeofficer.new_nominated_officer(form2, true)
//
//      noException must be thrownBy doc.getElementById("id1")
//      noException must be thrownBy doc.getElementById("id2")
//      noException must be thrownBy doc.getElementById("id3")
//
//    }
//
//    "show errors in the correct locations" in new ViewFixture {
//
//      val form2: InvalidForm = InvalidForm(Map.empty,
//        Seq(
//          (Path \ "blah") -> Seq(ValidationError("not a message Key")),
//          (Path \ "blah2") -> Seq(ValidationError("second not a message Key")),
//          (Path \ "blah3") -> Seq(ValidationError("third not a message Key"))
//        ))
//
//      def view = views.html.changeofficer.new_nominated_officer(form2, true)
//
//      errorSummary.html() must include("not a message Key")
//      errorSummary.html() must include("second not a message Key")
//      errorSummary.html() must include("third not a message Key")
//
//    }
  }
}
