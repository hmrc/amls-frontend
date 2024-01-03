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

package views

import config.ApplicationConfig
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import views.html.UnauthorisedView

class UnauthorisedViewSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val unauthorisedView = app.injector.instanceOf[UnauthorisedView]
    implicit val requestWithToken = addTokenForView()
    implicit val config = app.injector.instanceOf[ApplicationConfig]
    def view = unauthorisedView()(requestWithToken, config, messages)
  }

  "UnauthorisedView" must {
    "display the correct headings and titles" in new ViewFixture {
      validateTitle("unauthorised.title")
      doc.getElementsByTag("h1").text mustBe messages("unauthorised.title")
    }

    "display the correct body content" in new ViewFixture {
      val paragraphs = doc.getElementsByClass("govuk-body").text()
      paragraphs must include(messages("unauthorised.text1"))
      paragraphs must include(messages("unauthorised.text2"))
    }

    "display the correct link" in new ViewFixture {
      val link = doc.getElementById("register-link")

      link.text() mustBe messages("unauthorised.register.link.text")
      link.attr("href") mustBe config.registerNewOrgLink
    }

    "display the correct button with link" in new ViewFixture {
      val button = doc.getElementById("button")

      button.text() mustBe messages("button.backtosignin")
      button.attr("href") mustBe s"${config.logoutUrl}?continue=${config.loginContinue}"
    }
  }
}
