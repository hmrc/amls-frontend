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

import forms.{InvalidForm, ValidForm, Form2}
import models.responsiblepeople.ContactDetails
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class contact_detailsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "contact_details view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ContactDetails] = Form2(ContactDetails("0987654", "email.com"))

      def view = views.html.responsiblepeople.contact_details(form2, true, 1, None, "firstName lastName")

      doc.title must startWith(Messages("responsiblepeople.contact_details.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ContactDetails] = Form2(ContactDetails("0987654", "email.com"))

      def view = views.html.responsiblepeople.contact_details(form2, true, 1, None, "firstName lastName")

      heading.html must be(Messages("responsiblepeople.contact_details.heading", "firstName lastName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "phoneNumber") -> Seq(ValidationError("not a message Key")),
          (Path \ "emailAddress") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.responsiblepeople.contact_details(form2, true, 1, None, "firstName lastName")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("phoneNumber").parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("emailAddress").parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}