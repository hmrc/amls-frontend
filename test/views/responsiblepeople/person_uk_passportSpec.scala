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

package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class person_uk_passportSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "person_uk_passport view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      val name = "firstName lastName"

      def view = views.html.responsiblepeople.person_uk_passport(form2, true, 1, true, name)

      doc.title must startWith(Messages("responsiblepeople.uk.passport.title"))
      heading.html must be(Messages("responsiblepeople.uk.passport.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "ukPassport") must not be empty
      doc.getElementsByAttributeValue("name", "ukPassportNumber") must not be empty

    }
    "show errors in the correct locations and have the correct fields" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "ukPassport") -> Seq(ValidationError("not a message Key")),
          (Path \ "ukPassportNumber") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.responsiblepeople.person_uk_passport(form2, true, 1, true, "firstName lastName")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementsByAttributeValue("name", "ukPassport") must not be empty
      doc.getElementsByAttributeValue("name", "ukPassportNumber") must not be empty

    }
  }

}
