/*
 * Copyright 2025 HM Revenue & Customs
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

package views.renewal

import config.ApplicationConfig
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.YourResponsibilitiesView

class YourResponsibilitiesViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val yourResponsibilitiesView: YourResponsibilitiesView    = app.injector.instanceOf[YourResponsibilitiesView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
    implicit val config: ApplicationConfig                         = app.injector.instanceOf[ApplicationConfig]
    def view: HtmlFormat.Appendable                                = yourResponsibilitiesView()(requestWithToken, config, messages)
  }

  "YourResponsibilitiesView" must {
    "display the correct headings and titles" in new ViewFixture {
      validateTitle("renewal.yourresponsibilities.title")
      doc.getElementsByTag("h1").text mustBe messages("renewal.yourresponsibilities.title")
    }

    "display the correct body content" in new ViewFixture {
      val paragraphs: String = doc.getElementsByClass("govuk-body").text()
      paragraphs must include(messages("renewal.yourresponsibilities.text1"))
      paragraphs must include(messages("renewal.yourresponsibilities.text2"))
      paragraphs must include(messages("renewal.yourresponsibilities.text3"))
    }

    "display the correct link" in new ViewFixture {
      val link: Element = doc.getElementById("guidance-link")

      link.text() mustBe messages("renewal.yourresponsibilities.link.text")
      link.attr("href") mustBe config.guidanceLink
    }

    "display the correct button with link" in new ViewFixture {
      val button: Element = doc.getElementById("button")

      button.text() mustBe messages("button.continue")
      button.attr("href") mustBe "/anti-money-laundering/renewal/other-activities"
    }
  }
}
