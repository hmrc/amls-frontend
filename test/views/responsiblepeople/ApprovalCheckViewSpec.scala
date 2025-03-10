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

package views.responsiblepeople

import forms.responsiblepeople.ApprovalCheckFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.ApprovalCheckView

class ApprovalCheckViewSpec extends AmlsViewSpec with Matchers {

  lazy val approval_check = inject[ApprovalCheckView]
  lazy val fp             = inject[ApprovalCheckFormProvider]

  val name = "James Jones"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ApprovalCheckView" must {

    "have correct title" in new ViewFixture {

      def view = approval_check(fp(), true, 0, None, name)

      doc.title must be(
        messages("responsiblepeople.approval_check.title", name)
          + " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = approval_check(fp(), true, 0, None, name)

      heading.html    must be(messages("responsiblepeople.approval_check.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
      doc.title       must include(messages("responsiblepeople.approval_check.title"))
    }

    "have the correct content" when {
      "details label is shown" in new ViewFixture {

        def view = approval_check(fp(), true, 0, None, name)
        doc.body().html() must include(messages("responsiblepeople.approval_check.text.details"))
        doc.body().html() must include(messages("responsiblepeople.approval_check.text.details2", name))
      }
    }

    behave like pageWithErrors(
      approval_check(
        fp().withError("hasAlreadyPaidApprovalCheck", "error.required.rp.approval_check"),
        true,
        0,
        None,
        name
      ),
      "hasAlreadyPaidApprovalCheck",
      "error.required.rp.approval_check"
    )

    behave like pageWithBackLink(approval_check(fp(), true, 0, None, name))
  }
}
