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

package views.tcsp

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.services_of_another_tcsp


class services_of_another_tcspSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val services_of_another_tcsp = app.injector.instanceOf[services_of_another_tcsp]
    implicit val requestWithToken = addTokenForView()
  }

  "services_of_another_tcsp view" must {
    "have correct title, correct heading and subheading" in new ViewFixture {

      val form2 = EmptyForm

      def view = services_of_another_tcsp(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      val title = Messages("tcsp.servicesOfAnotherTcsp.title") + " - " + Messages("summary.tcsp") + " - " +
                  Messages("title.amls") + " - " + Messages("title.gov")
      doc.title must be(title)
      heading.html must be(Messages("tcsp.servicesOfAnotherTcsp.title"))
      subHeading.html must include(Messages("summary.tcsp"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "servicesOfAnotherTCSP") -> Seq(ValidationError("not a message Key"))
        ))

      def view = services_of_another_tcsp(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("servicesOfAnotherTCSP")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}