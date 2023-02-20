/*
 * Copyright 2023 HM Revenue & Customs
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

package views.hvd

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.hvd
import models.hvd.CashPaymentOverTenThousandEuros
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.cash_payment


class cash_payment_over_ten_thousand_eurosSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val cash_payment = app.injector.instanceOf[cash_payment]
    implicit val requestWithToken = addTokenForView()
  }

  "cash_payment view" must {

    "have the back link button" in new ViewFixture {

      val form2: ValidForm[CashPaymentOverTenThousandEuros] = Form2(CashPaymentOverTenThousandEuros(true))

      def view = cash_payment(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[CashPaymentOverTenThousandEuros] = Form2(hvd.CashPaymentOverTenThousandEuros(true))

      def view = cash_payment(form2, true)

      doc.title must startWith(Messages("hvd.cash.payment.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CashPaymentOverTenThousandEuros] = Form2(hvd.CashPaymentOverTenThousandEuros(true))

      def view = cash_payment(form2, true)

      heading.html must be(Messages("hvd.cash.payment.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "acceptedAnyPayment") -> Seq(ValidationError("not a message Key"))
        ))

      def view = cash_payment(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("acceptedAnyPayment")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
