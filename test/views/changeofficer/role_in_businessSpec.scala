/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessType
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class role_in_businessSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"
  }

  "position_within_business view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.changeofficer.role_in_business(form2, BusinessType.SoleProprietor, name)

      doc.title must be(Messages("changeofficer.roleinbusiness.title") +
        " - " + Messages("summary.updateinformation") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("changeofficer.roleinbusiness.heading", name))
      subHeading.html must include(Messages("summary.updateinformation"))

      doc.getElementsByAttributeValue("name", "positions[]") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "positions") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.changeofficer.role_in_business(form2, BusinessType.SoleProprietor, name)

      errorSummary.html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {
      def view = views.html.changeofficer.role_in_business(EmptyForm, BusinessType.SoleProprietor, name)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}