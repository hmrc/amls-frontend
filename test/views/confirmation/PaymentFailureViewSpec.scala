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

package views.confirmation

import models.confirmation.Currency
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class PaymentFailureViewSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    //noinspection ScalaStyle
    override def view = views.html.confirmation.payment_failure("confirmation.payment.failed.reason.failure", 100, "X123456789")
  }

  "The Payment Failure view" must {
    "show the correct headings and content" in new ViewFixture {
      validateTitle("confirmation.payment.failed.title")
      doc.body.select(".page-header h1").text() mustBe Messages("confirmation.payment.failed.header")
      validateParagraphizedContent("confirmation.payment.failed.reason.failure")
      validateParagraphizedContent("confirmation.payment.failed.info")
    }

    "show the fee and reference" in new ViewFixture {
      //noinspection ScalaStyle
      doc.body.select(".payment-amount").text() mustBe Currency(100).toString
      doc.body.select(".payment-ref").text() mustBe "X123456789"
    }
  }

}
