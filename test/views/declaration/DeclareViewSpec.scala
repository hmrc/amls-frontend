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
import views.html.declaration.DeclareView

class DeclareViewSpec extends AmlsViewSpec with Matchers {

  lazy val declare = app.injector.instanceOf[DeclareView]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "declaration view" must {
    "have correct title" in new ViewFixture {
      def view = declare("string1", "string2", "Name", isAmendment = false)

      doc.title mustBe s"string1 - ${messages("title.amls")} - ${messages("title.gov")}"
    }

    "have correct headings" in new ViewFixture {

      def view = declare("string1", "string2", "Name", isAmendment = false)

      heading.html    must be(messages("declaration.declaration.title"))
      subHeading.html must include(messages("string2"))
    }

    "have correct content" in new ViewFixture {
      def view = declare("string1", "string2", "Name", isAmendment = false)

      doc.text() must include(messages("declaration.declaration.declare"))

      Seq(
        messages("declaration.declaration.tellhmrc"),
        messages("declaration.declaration.noncompliance"),
        messages("declaration.declaration.confirm"),
        messages("declaration.declaration.correctinformation")
      ) foreach { msg =>
        doc.select(".govuk-list.govuk-list--bullet").text() must include(msg)
      }
    }

    "have correct preamble when an 'amendment' message is passed in" in new ViewFixture {
      def view = declare("string1", "string2", "Name", isAmendment = true)

      doc.text() must include(messages("declaration.declaration.amendment.correctinformation"))
    }

    "display the person's name in the first line of the declaration text" in new ViewFixture {
      val name = "Some Person"
      def view = declare("string1", "string2", name, isAmendment = false)

      doc.select(".govuk-warning-text__text").text() must include(messages("declaration.declaration.fullname", name))
    }

    "have a form with the disable-on-submit attribute" in new ViewFixture {
      def view = declare("string1", "string2", "Name", isAmendment = false)

      doc.select("form").attr("disable-on-submit") mustBe "true"
    }

    behave like pageWithBackLink(declare("string1", "string2", "John Smith", isAmendment = false))
  }
}
