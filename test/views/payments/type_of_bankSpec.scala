/*
 * Copyright 2018 HM Revenue & Customs
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

package views.payments

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class type_of_bankSpec extends PlaySpec with GenericTestHelper{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

    "type_of_bank view" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = views.html.payments.type_of_bank(EmptyForm)

      doc.title must startWith(Messages("payments.typeofbank.title"))
      heading.html must be(Messages("payments.typeofbank.header"))
      subHeading.html must include(Messages("submit.registration"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty, Seq(
        (Path \ "typeOfBank") -> Seq(ValidationError("not a message Key"))
      ))

      def view = views.html.payments.type_of_bank(form2)

      errorSummary.html() must include("not a message Key")
    }

    "display all fields" in new ViewFixture {

      def view = views.html.payments.type_of_bank(EmptyForm)

      doc.getElementsByAttributeValue("for", "typeOfBank-true") must not be empty
      doc.getElementsByAttributeValue("for", "typeOfBank-false") must not be empty

    }
  }

}
