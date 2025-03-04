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

package views.deregister

import forms.deregister.DeregistrationReasonFormProvider
import models.deregister.DeregistrationReason
import models.deregister.DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.deregister.DeregistrationReasonView

class DeregistrationReasonViewSpec extends AmlsViewSpec with Matchers {

  trait TestFixture extends Fixture

  lazy val deregistration_reason                                 = inject[DeregistrationReasonView]
  lazy val fp                                                    = inject[DeregistrationReasonFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  "DeregistrationReasonView" must {
    "have correct title" in new TestFixture {

      def view = deregistration_reason(fp())

      doc.title must be(
        Messages("deregistration.reason.heading") +
          " - " + Messages("title.yapp") +
          " - " + Messages("title.amls") +
          " - " + Messages("title.gov")
      )
    }

    "have correct headings" in new TestFixture {

      def view = deregistration_reason(fp())

      heading.html    must be(Messages("deregistration.reason.heading"))
      subHeading.html must include(Messages("summary.status"))

    }

    "have the correct fields" when {
      "HVD is required" in new TestFixture {

        override def view = deregistration_reason(fp(), true)

        DeregistrationReason.all foreach { reason =>
          doc.getElementsByAttributeValue("for", reason.toString) must not be empty
        }

        doc.getElementsByAttributeValue("name", "specifyOtherReason") must not be empty
      }

      "HVD is not required" in new TestFixture {

        override def view = deregistration_reason(fp())

        DeregistrationReason.all diff Seq(HVDPolicyOfNotAcceptingHighValueCashPayments) foreach { reason =>
          doc.getElementsByAttributeValue("for", reason.toString) must not be empty
        }

        doc.getElementsByAttributeValue("for", HVDPolicyOfNotAcceptingHighValueCashPayments.toString) must be(empty)
        doc.getElementsByAttributeValue("name", "specifyOtherReason")                                 must not be empty
      }

    }

    "have a form with the disable-on-submit attribute" in new TestFixture {
      def view = deregistration_reason(fp())

      doc.select("form").attr("disable-on-submit") mustBe "true"
    }

    behave like pageWithErrors(
      deregistration_reason(fp().withError("deregistrationReason", "error.required.deregistration.reason")),
      "deregistrationReason",
      "error.required.deregistration.reason"
    )

    behave like pageWithErrors(
      deregistration_reason(fp().withError("specifyOtherReason", "error.required.deregistration.reason.format")),
      "specifyOtherReason",
      "error.required.deregistration.reason.format"
    )

    behave like pageWithBackLink(deregistration_reason(fp()))
  }
}
