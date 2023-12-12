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

package views.businessmatching

import forms.businessmatching.TypeOfBusinessFormProvider
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.TypeOfBusiness
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import org.jsoup.nodes.Element
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import views.Fixture
import views.html.businessmatching.TypeOfBusinessView


class TypeOfBusinessViewSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val type_of_business = app.injector.instanceOf[TypeOfBusinessView]
    lazy val formProvider = app.injector.instanceOf[TypeOfBusinessFormProvider]
    implicit val requestWithToken = addTokenForView()
  }

  "type_of_business view" must {
    "have correct title" in new ViewFixture {

      val filledForm: Form[TypeOfBusiness] = formProvider().fill(TypeOfBusiness("Business Type"))

      def view = type_of_business(filledForm, edit = true)

      doc.title must startWith(messages("businessmatching.typeofbusiness.title") + " - " + messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      val filledForm = formProvider().fill(TypeOfBusiness("Business Type"))

      def view = type_of_business(filledForm, edit = true)

      heading.html must include(messages("businessmatching.typeofbusiness.title"))
      subHeading.html must include(messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val messageKey = "error.required.bm.businesstype.type"

      val invalidForm = formProvider().withError(FormError("typeOfBusiness", messageKey))

      def view = type_of_business(invalidForm, edit = true)

      doc.getElementsByClass("govuk-error-summary").text() must include(messages(messageKey))

      doc.getElementById("typeOfBusiness-error").text() must include(messages(messageKey))

    }

    "have a back link" in new ViewFixture {
      def view = type_of_business(formProvider(), edit = true)

      doc.getElementById("back-link").isInstanceOf[Element] mustBe true
    }
  }
}