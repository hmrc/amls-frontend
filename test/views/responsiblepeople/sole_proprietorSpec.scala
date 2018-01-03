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

package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.responsiblepeople.{SoleProprietorOfAnotherBusiness, ResponsiblePeople}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class sole_proprietorSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "sole_proprietor view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[SoleProprietorOfAnotherBusiness] = Form2(SoleProprietorOfAnotherBusiness(true))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, None, "Person Name")

      doc.title must be(Messages("responsiblepeople.sole.proprietor.another.business.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))

      doc.getElementsByAttributeValue("name", "soleProprietorOfAnotherBusiness") must not be empty
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[SoleProprietorOfAnotherBusiness] = Form2(SoleProprietorOfAnotherBusiness(true))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, None, "Person Name")

      heading.html must be(Messages("responsiblepeople.sole.proprietor.another.business.heading", "Person Name"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "soleProprietorOfAnotherBusiness") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, None, "Person Name")

      errorSummary.html() must include("not a message Key")

    }
  }
}