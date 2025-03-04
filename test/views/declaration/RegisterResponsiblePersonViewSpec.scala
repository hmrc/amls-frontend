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

package views.declaration

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.RegisterResponsiblePersonView

class RegisterResponsiblePersonViewSpec extends AmlsViewSpec with Matchers {

  lazy val personView = app.injector.instanceOf[RegisterResponsiblePersonView]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "RegisterResponsiblePersonView" must {
    "Have the correct title" in new ViewFixture {
      def view = personView("subheading")

      doc.title must startWith(messages("declaration.register.responsible.person.title"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = personView("subheading")

      heading.html    must be(messages("declaration.register.responsible.person.title"))
      subHeading.html must include(messages("subheading"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = personView("subheading")

      html must include(messages("declaration.register.responsible.person.description"))
      html must include(messages("declaration.register.responsible.person.title"))

      html must include(messages("subheading"))

      html must include(messages("declaration.register.responsible.person.text"))
      html must include(messages("owners, partners, directors, shadow directors, and designated members"))
      html must include(messages("the nominated officer for your business"))
      html must include(messages("beneficial owners or shareholders who own or control more than 25% of the business"))
      html must include(messages("other officers of the business, like the company secretary"))
      html must include(messages("senior managers of activities covered by the Money Laundering Regulations"))
    }

    behave like pageWithBackLink(personView("subheading"))
  }
}
