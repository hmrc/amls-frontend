/*
 * Copyright 2021 HM Revenue & Customs
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
import models.businessdetails.{RegisteredOffice, RegisteredOfficeUK}
import org.scalatest.MustMatchers
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture
import views.html.businessdetails.registered_office_is_uk


class registered_office_is_ukSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    lazy val registered_office_is_uk = app.injector.instanceOf[registered_office_is_uk]
    implicit val requestWithToken = addTokenForView()
  }

  "registered_office view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[RegisteredOffice] = Form2(RegisteredOfficeUK("line1","line2",None,None,"AB12CD"))

      def view = registered_office_is_uk(form2, true)

      doc.title must startWith(Messages("businessdetails.registeredoffice.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[RegisteredOffice] = Form2(RegisteredOfficeUK("line1","line2",None,None,"AB12CD"))

      def view = registered_office_is_uk(form2, true)

      heading.html must be(Messages("businessdetails.registeredoffice.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("not a message Key"))
        ))

      def view = registered_office_is_uk(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("isUK")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = registered_office_is_uk(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}