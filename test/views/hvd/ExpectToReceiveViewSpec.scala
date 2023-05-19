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

import forms.hvd.ExpectToReceiveFormProvider
import models.hvd.PaymentMethods
import org.scalatest.MustMatchers
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.ExpectToReceiveView

class ExpectToReceiveViewSpec extends AmlsViewSpec with MustMatchers  {

  lazy val receiveView = inject[ExpectToReceiveView]
  lazy val fp = inject[ExpectToReceiveFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
      implicit val requestWithToken = addTokenForView()
    }

    "ExpectToReceiveView" must {

      "have correct title" in new ViewFixture {

        def view = receiveView(fp().fill(PaymentMethods(true, true, None)), true)

        doc.title must startWith (messages("hvd.expect.to.receive.title") + " - " + messages("summary.hvd"))
      }

      "have correct headings" in new ViewFixture {

        def view = receiveView(fp().fill(PaymentMethods(true, true, None)), true)

        heading.html must be (messages("hvd.expect.to.receive.title"))
        subHeading.html must include (messages("summary.hvd"))

      }

      behave like pageWithErrors(
        receiveView(fp().withError("paymentMethods", "error.required.hvd.choose.option"), false),
        "paymentMethods",
        "error.required.hvd.choose.option"
      )

      behave like pageWithErrors(
        receiveView(fp().withError("details", "error.required.hvd.format"), false),
        "details",
        "error.required.hvd.format"
      )
    }
  }
