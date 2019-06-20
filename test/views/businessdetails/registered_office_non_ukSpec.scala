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

package views.businessdetails

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessdetails.{RegisteredOffice, RegisteredOfficeUK}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsSpec, AutoCompleteServiceMocks}
import views.Fixture


class registered_office_non_ukSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    implicit val requestWithToken = addToken(request)
  }

  "registered_office view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[RegisteredOffice] = Form2(RegisteredOfficeUK("line1","line2",None,None,"AB12CD"))

      def view = views.html.businessdetails.registered_office_non_uk(form2, true, mockAutoComplete.getCountries)

      doc.title must startWith(Messages("businessdetails.registeredoffice.where.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[RegisteredOffice] = Form2(RegisteredOfficeUK("line1","line2",None,None,"AB12CD"))

      def view = views.html.businessdetails.registered_office_non_uk(form2, true, mockAutoComplete.getCountries)

      heading.html must be(Messages("businessdetails.registeredoffice.where.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "country-fieldset") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessdetails.registered_office_non_uk(form2, true, mockAutoComplete.getCountries)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("country-fieldset")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = views.html.businessdetails.registered_office_non_uk(form2, true, mockAutoComplete.getCountries)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}