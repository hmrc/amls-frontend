/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessFranchise, BusinessFranchiseNo, BusinessFranchiseYes}
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture
import views.html.businessactivities.business_franchise_name


class business_franchise_nameSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val franchise = app.injector.instanceOf[business_franchise_name]
    implicit val requestWithToken = addTokenForView()
  }

  "business_franchise_name view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessFranchise] = Form2(BusinessFranchiseYes("Franchise name"))

      def view = franchise(form2, true)

      doc.title must startWith(Messages("businessactivities.businessfranchise.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {
      val form2: ValidForm[BusinessFranchise] = Form2(BusinessFranchiseNo)

      def view = franchise(form2, true)

      heading.html must be(Messages("businessactivities.businessfranchise.title"))
      subHeading.html must include(Messages("summary.businessactivities"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "franchiseName") -> Seq(ValidationError("not a message Key")),
          (Path \ "businessFranchise") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = franchise(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("franchiseName-panel")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("businessFranchise")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
    }

    "have a back link" in new ViewFixture {
      def view = franchise(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
