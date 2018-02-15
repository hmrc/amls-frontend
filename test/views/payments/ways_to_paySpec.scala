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
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class ways_to_paySpec extends GenericTestHelper {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "ways_to_pay view" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = views.html.payments.ways_to_pay(EmptyForm)

      doc.title must startWith(Messages("payments.waystopay.title"))
      heading.html must be(Messages("payments.waystopay.header"))
      subHeading.html must include(Messages("submit.registration"))
      doc.html must include(Messages("payments.waystopay.info"))
      doc.html must include(Messages("payments.waystopay.info2"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty, Seq(
        (Path \ "waysToPay") -> Seq(ValidationError("not a message Key"))
      ))

      def view = views.html.payments.ways_to_pay(form2)

      errorSummary.html() must include("not a message Key")
    }

    "display all fields" in new ViewFixture {

      def view = views.html.payments.ways_to_pay(EmptyForm)

      doc.getElementsByAttributeValue("for", "waysToPay-card") must not be empty
      doc.getElementsByAttributeValue("for", "waysToPay-bacs") must not be empty

    }
  }
}
