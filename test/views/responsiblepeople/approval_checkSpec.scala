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

import forms.{EmptyForm, Form2, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class approval_checkSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "approval_check view" must {
    "have correct title" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.responsiblepeople.approval_check(form2, true, 0, None, "PersonName")

      doc.title must be(
        Messages("responsiblepeople.approval_check.title", "PersonName")
        + " - " + Messages("summary.responsiblepeople")+
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.responsiblepeople.approval_check(form2, true, 0, None, "PersonName")

      heading.html must be(Messages("responsiblepeople.approval_check.heading", "PersonName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))
      doc.title must include(Messages("responsiblepeople.approval_check.title"))
    }

    "have the correct content" when {
      "details label is shown" in new ViewFixture {

        val form2: Form2[_] = EmptyForm

        def view = views.html.responsiblepeople.approval_check(form2, true, 0, None, "PersonName")
        doc.body().html() must include(Messages("responsiblepeople.approval_check.text.details"))
      }
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasAlreadyPaidApprovalCheck") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.approval_check(form2, true, 0, None, "PersonName")

      errorSummary.html() must include("not a message Key")

      doc.getElementById("hasAlreadyPaidApprovalCheck")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
