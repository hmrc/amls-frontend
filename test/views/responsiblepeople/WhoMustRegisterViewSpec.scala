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

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.WhoMustRegisterView

class WhoMustRegisterViewSpec extends AmlsViewSpec with Matchers {

  lazy val who_must_register = inject[WhoMustRegisterView]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhoMustRegisterView View" must {

    "Have the correct title" in new ViewFixture {
      def view = who_must_register(1)

      doc.title must be(
        messages("responsiblepeople.whomustregister.ymr") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "Have the correct Headings" in new ViewFixture {
      def view = who_must_register(1)

      heading.html    must be(messages("responsiblepeople.whomustregister.ymr"))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = who_must_register(1)

      html must include(messages("declaration.register.responsible.person.line_1"))
      html must include(messages("declaration.register.responsible.person.line_2"))
      html must include(messages("declaration.register.responsible.person.line_3"))
      html must include(messages("declaration.register.responsible.person.line_4"))
      html must include(messages("declaration.register.responsible.person.line_5"))
    }

    behave like pageWithBackLink(who_must_register(1))
  }
}
