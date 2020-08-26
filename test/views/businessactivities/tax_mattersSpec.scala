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

package views.businessactivities

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.TaxMatters
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture
import views.html.businessactivities.tax_matters


class tax_mattersSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val matters = app.injector.instanceOf[tax_matters]
    implicit val requestWithToken = addTokenForView()
    val accountantName = "Accountant name"
  }

  "tax_matters view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[TaxMatters] = Form2(TaxMatters(true))

      def view = matters(form2, true, accountantName)

      doc.title must startWith(Messages("businessactivities.tax.matters.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[TaxMatters] = Form2(TaxMatters(true))

      def view = matters(form2, true, accountantName)

      heading.html must be(Messages("businessactivities.tax.matters.heading", accountantName))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "manageYourTaxAffairs") -> Seq(ValidationError("not a message Key"))
        ))

      def view = matters(form2, true, accountantName)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("manageYourTaxAffairs")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "have a back link" in new ViewFixture {
      def view = matters(EmptyForm, true, accountantName)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}