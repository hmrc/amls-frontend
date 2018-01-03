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

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class new_home_date_of_changeSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"
  }

  "new_home_date_of_change view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("responsiblepeople.new.home.date.of.change.title") + " - " +
        Messages("summary.responsiblepeople") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.responsiblepeople.new_home_date_of_change(form2, 1, name)

      doc.title must be(pageTitle)
      heading.html must be(Messages("responsiblepeople.new.home.date.of.change.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsContainingOwnText(Messages("lbl.day")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("lbl.month")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("lbl.year")).hasText must be(true)

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.new_home_date_of_change(form2, 1, name)

      errorSummary.html() must include("not a message Key")
    }
  }
}