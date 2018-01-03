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

import forms.{InvalidForm, ValidForm, Form2}
import models.hvd.{PaymentMethods, ReceiveCashPayments}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class receivingSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "receiving view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ReceiveCashPayments] = Form2(ReceiveCashPayments(Some(PaymentMethods(true, true, None))))

      def view = views.html.hvd.receiving(form2, true)

      doc.title must startWith (Messages("hvd.receiving.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ReceiveCashPayments] = Form2(ReceiveCashPayments(Some(PaymentMethods(true, true, None))))

      def view = views.html.hvd.receiving(form2, true)

      heading.html must be (Messages("hvd.receiving.title"))
      subHeading.html must include (Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "receivePayments") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.hvd.receiving(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("receivePayments").html() must include("not a message Key")

    }
  }
}
