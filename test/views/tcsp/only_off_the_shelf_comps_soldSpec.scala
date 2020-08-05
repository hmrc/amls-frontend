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

package views.tcsp

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.only_off_the_shelf_comps_sold


class only_off_the_shelf_comps_soldSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val only_off_the_shelf_comps_sold = app.injector.instanceOf[only_off_the_shelf_comps_sold]
    implicit val requestWithToken = addTokenForView()
  }

  "only_off_the_shelf_comps_sold view" must {
    "have correct title, heading amd subheading" in new ViewFixture {

      val form2 = EmptyForm

      def view = only_off_the_shelf_comps_sold(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      val title = Messages("tcsp.off-the-shelf.companies.lbl") + " - " + Messages("summary.tcsp") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
      heading.html must be(Messages("tcsp.off-the-shelf.companies.lbl"))
      subHeading.html must include(Messages("summary.tcsp"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "onlyOffTheShelfCompsSold") -> Seq(ValidationError("not a message Key"))
        ))

      def view = only_off_the_shelf_comps_sold(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("onlyOffTheShelfCompsSold")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}