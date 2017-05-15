/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.{InvalidForm, ValidForm, Form2}
import models.hvd.LinkedCashPayments
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class linked_cash_paymentsSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "linked_cash_payments view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[LinkedCashPayments] = Form2(LinkedCashPayments(true))

      def view = views.html.hvd.linked_cash_payments(form2, true)

      doc.title must startWith(Messages("hvd.identify.linked.cash.payment.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[LinkedCashPayments] = Form2(LinkedCashPayments(false))

      def view = views.html.hvd.linked_cash_payments(form2, true)

      heading.html must be(Messages("hvd.identify.linked.cash.payment.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "linkedCashPayments") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.hvd.linked_cash_payments(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("linkedCashPayments")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
