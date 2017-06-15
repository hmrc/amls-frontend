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

package views.aboutthebusiness

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.ContactingYouPhone
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class contacting_you_phoneSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "contacting_you view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      val form2: ValidForm[ContactingYouPhone] = Form2(ContactingYouPhone("123456789789"))

      def view = {
        views.html.aboutthebusiness.contacting_you_phone(form2, true)
      }

      doc.title must be(Messages("aboutthebusiness.contactingyou.phone.title") +
        " - " + Messages("summary.aboutbusiness") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("aboutthebusiness.contactingyou.phone.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

      doc.getElementsByAttributeValue("name", "phoneNumber") must not be empty

    }

    "show error summary in correct location" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "contactingyou-fieldset") -> Seq(ValidationError("not a message Key"))
        ))

      def view = {
        views.html.aboutthebusiness.contacting_you_phone(form2, true)
      }

      errorSummary.html() must include("not a message Key")

    }
  }
}