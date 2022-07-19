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

package views.renewal

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.PercentageOfCashPaymentOver15000
import models.renewal.PercentageOfCashPaymentOver15000.{Second, Third}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.percentage

class percentageSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val percentage = app.injector.instanceOf[percentage]
    implicit val requestWithToken = addTokenForView()
  }

  "percentage view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[PercentageOfCashPaymentOver15000] = Form2(Second)

      def view = percentage(form2, true)

      doc.title must startWith(Messages("renewal.hvd.percentage.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[PercentageOfCashPaymentOver15000] = Form2(Third)

      def view = percentage(form2, true)

      heading.html must be(Messages("renewal.hvd.percentage.title"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "percentage") -> Seq(ValidationError("not a message Key"))
        ))

      def view = percentage(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("percentage")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = percentage(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
