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
import models.hvd.CashPaymentFirstDate
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.cash_payment_first_date


class cash_payment_first_dateSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val cash_payment_first_date = app.injector.instanceOf[cash_payment_first_date]
    implicit val requestWithToken = addTokenForView()
  }

  "cash_payment view" must {

    "have the back link button" in new ViewFixture {

      val form2: ValidForm[CashPaymentFirstDate] = Form2(CashPaymentFirstDate(LocalDate.parse("2016-3-20")))

      def view = cash_payment_first_date(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[CashPaymentFirstDate] = Form2(CashPaymentFirstDate(LocalDate.parse("2016-3-20")))

      def view = cash_payment_first_date(form2, true)

      doc.title must startWith(Messages("hvd.cash.payment.date.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CashPaymentFirstDate] = Form2(CashPaymentFirstDate(LocalDate.parse("2016-3-24")))

      def view = cash_payment_first_date(form2, true)

      heading.html must be(Messages("hvd.cash.payment.date.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "paymentDate") -> Seq(ValidationError("not a message Key"))
        ))

      def view = cash_payment_first_date(form2, true)

      errorSummary.html() must include("not a message Key")


      doc.getElementById("paymentDate")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
