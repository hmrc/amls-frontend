/*
 * Copyright 2024 HM Revenue & Customs
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

package views.withdrawal

import forms.withdrawal.WithdrawalReasonFormProvider
import models.withdrawal.WithdrawalReason
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.withdrawal.WithdrawalReasonView

class WithdrawalReasonViewSpec extends AmlsViewSpec with Matchers {

  lazy val withdrawal_reason                                     = inject[WithdrawalReasonView]
  lazy val fp                                                    = inject[WithdrawalReasonFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  trait TestFixture extends Fixture

  "WithdrawalReasonView" must {
    "have correct title" in new TestFixture {

      def view = withdrawal_reason(fp())

      doc.title must be(
        messages("withdrawal.reason.heading") +
          " - " + messages("title.yapp") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new TestFixture {

      def view = withdrawal_reason(fp())

      heading.html    must be(messages("withdrawal.reason.heading"))
      subHeading.html must include(messages("summary.status"))
    }

    "have the correct fields" in new TestFixture {

      override def view = withdrawal_reason(fp())

      WithdrawalReason.all.foreach { reason =>
        doc.getElementsByAttributeValue("for", reason.toString) must not be empty
      }

      doc.getElementsByAttributeValue("name", "specifyOtherReason") must not be empty
    }

    "have a form with the disable-on-submit attribute" in new TestFixture {
      def view = withdrawal_reason(fp())

      doc.select("form").attr("disable-on-submit") mustBe "true"
    }

    behave like pageWithErrors(
      withdrawal_reason(fp().withError("withdrawalReason", "error.required.withdrawal.reason")),
      "withdrawalReason",
      "error.required.withdrawal.reason"
    )

    behave like pageWithErrors(
      withdrawal_reason(fp().withError("specifyOtherReason", "error.required.withdrawal.reason.format")),
      "specifyOtherReason",
      "error.required.withdrawal.reason.format"
    )

    behave like pageWithBackLink(withdrawal_reason(fp()))
  }
}
