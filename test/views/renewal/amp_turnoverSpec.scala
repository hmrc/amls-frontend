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

package views.renewal

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.AMPTurnover
import models.renewal.AMPTurnover.{Second, Third}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture

class amp_turnoverSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "ampTurnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AMPTurnover] = Form2(Second)

      def view = views.html.renewal.amp_turnover(form2, true)

      doc.title must startWith("How much of your turnover for the last 12 months came from sales of art for €10,000 or more? - Renewal - Manage your anti-money laundering supervision - GOV.UK")
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AMPTurnover] = Form2(Third)

      def view = views.html.renewal.amp_turnover(form2, true)

      heading.html must be("How much of your turnover for the last 12 months came from sales of art for €10,000 or more?")
      subHeading.html must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "percentageExpectedTurnover") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.amp_turnover(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("percentageExpectedTurnover")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = views.html.renewal.amp_turnover(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
