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

package views.renewal

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.PercentageOfCashPaymentOver15000
import models.renewal.PercentageOfCashPaymentOver15000.{Second, Third}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class percentageSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "percentage view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[PercentageOfCashPaymentOver15000] = Form2(Second)

      def view = views.html.hvd.percentage(form2, true)

      doc.title must startWith(Messages("hvd.percentage.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[PercentageOfCashPaymentOver15000] = Form2(Third)

      def view = views.html.hvd.percentage(form2, true)

      heading.html must be(Messages("hvd.percentage.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "percentage") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.hvd.percentage(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("percentage")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
