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

package views.supervision

import forms.supervision.MemberOfProfessionalBodyFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.MemberOfProfessionalBodyView

class MemberOfProfessionalBodyViewSpec extends AmlsViewSpec with Matchers {

  lazy val member_of_professional_body = inject[MemberOfProfessionalBodyView]
  lazy val fp                          = app.injector.instanceOf[MemberOfProfessionalBodyFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "MemberOfProfessionalBodyView" must {

    "have the correct title, heading, subtitle and form" in new ViewFixture {

      def view = member_of_professional_body(fp(), edit = false)

      doc.title       must startWith(messages("supervision.memberofprofessionalbody.title"))
      heading.html    must include(messages("supervision.memberofprofessionalbody.title"))
      subHeading.html must include(messages("summary.supervision"))

      doc.getElementById("isAMember").tag().getName mustBe "input"
      doc.getElementById("isAMember-2").tag().getName mustBe "input"
    }

    behave like pageWithErrors(
      member_of_professional_body(
        fp().withError("isAMember", "error.required.supervision.business.a.member"),
        true
      ),
      "isAMember",
      "error.required.supervision.business.a.member"
    )

    behave like pageWithBackLink(member_of_professional_body(fp(), false))
  }
}
