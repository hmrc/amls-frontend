/*
 * Copyright 2022 HM Revenue & Customs
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

package views.businessactivities

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.nca_registered


class nca_registeredSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val registered = app.injector.instanceOf[nca_registered]
    implicit val requestWithToken = addTokenForView()
  }

  "nca_registered view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = registered(form2, true)

      doc.title must be(Messages("businessactivities.ncaRegistered.title") + " - " +
        Messages("summary.businessactivities") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = registered(form2, true)

      heading.html must be(Messages("businessactivities.ncaRegistered.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = registered(form2, true)

      doc.getElementsByAttributeValue("name", "ncaRegistered") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "ncaRegistered") -> Seq(ValidationError("not a message Key"))
        ))

      def view = registered(form2, true)

      errorSummary.html() must include("not a message Key")

    }

    "have a back link" in new ViewFixture {
      def view = registered(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}