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

package views.hvd

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.hvd.{PaymentMethods, ReceiveCashPayments}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class expect_to_receiveSpec extends GenericTestHelper with MustMatchers  {

    trait ViewFixture extends Fixture {
      implicit val requestWithToken = addToken(request)
    }

    "expect_to_receive view" must {
      "have correct title" in new ViewFixture {

        val form2: ValidForm[ReceiveCashPayments] = Form2(ReceiveCashPayments(Some(PaymentMethods(true, true, None))))

        def view = views.html.hvd.expect_to_receive(form2, true)

        doc.title must startWith (Messages("hvd.expect.to.receive.title") + " - " + Messages("summary.hvd"))
      }

      "have correct headings" in new ViewFixture {

        val form2: ValidForm[ReceiveCashPayments] = Form2(ReceiveCashPayments(Some(PaymentMethods(true, true, None))))

        def view = views.html.hvd.expect_to_receive(form2, true)

        heading.html must be (Messages("hvd.expect.to.receive.title"))
        subHeading.html must include (Messages("summary.hvd"))

      }

      "show errors in the correct locations" in new ViewFixture {

        val form2: InvalidForm = InvalidForm(Map.empty,
          Seq(
            (Path \ "paymentMethods") -> Seq(ValidationError("second not a message Key")),
            (Path \ "paymentMethods-details-fieldset") -> Seq(ValidationError("third not a message Key"))
          ))

        def view = views.html.hvd.expect_to_receive(form2, true)

        errorSummary.html() must include("second not a message Key")
        errorSummary.html() must include("third not a message Key")

        doc.getElementById("paymentMethods").html() must include("second not a message Key")
        doc.getElementById("paymentMethods-details-fieldset").html() must include("third not a message Key")
      }
    }
  }
