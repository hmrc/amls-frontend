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

import forms.{EmptyForm, InvalidForm, ValidForm, Form2}
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class trainingSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "training view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.training(form2, false, 0, false, "Person Name")

      doc.title must be(Messages("responsiblepeople.training.title") + " - " +
        Messages("summary.responsiblepeople")+
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.training(form2, false, 0, false, "Person Name")

      heading.html must be(Messages("responsiblepeople.training.heading", "Person Name"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.training(form2, false, 0, false, "Person Name")

      noException must be thrownBy doc.getElementById("training-true")
      noException must be thrownBy doc.getElementById("training-false")
      noException must be thrownBy doc.getElementById("information-fieldset")

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "training") -> Seq(ValidationError("not a message Key")),
          (Path \ "information") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.responsiblepeople.training(form2, false, 0, false, "Person Name")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

    }
  }
}