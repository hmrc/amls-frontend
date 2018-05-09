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

package views.aboutthebusiness

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.{ConfirmRegisteredOffice, RegisteredOfficeUK}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class letters_addressSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "letters_address view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      val form2: ValidForm[ConfirmRegisteredOffice] = Form2(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.letters_address(form2, address, true)
      }

      doc.title must be(Messages("aboutthebusiness.lettersaddress.title") +
        " - " + Messages("summary.aboutbusiness") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("aboutthebusiness.lettersaddress.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

      doc.getElementsMatchingOwnText("line1").text mustBe "line1 line2 AB12CD"
      doc.select("input[type=radio]").size mustBe 2
    }

    "show error summary in correct location" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "lettersAddress") -> Seq(ValidationError("not a message Key"))
        ))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.letters_address(form2, address, true)
      }

      errorSummary.html() must include("not a message Key")

    }
  }
}