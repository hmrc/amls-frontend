/*
 * Copyright 2026 HM Revenue & Customs
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
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.html.UnauthorisedAuthProviderView

class UnauthorisedAuthProviderViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val unauthorisedAuthProviderView: UnauthorisedAuthProviderView =
      app.injector.instanceOf[UnauthorisedAuthProviderView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type]      = addTokenForView()
    implicit val config: ApplicationConfig                              = app.injector.instanceOf[ApplicationConfig]
    def view: HtmlFormat.Appendable                                     =
      unauthorisedAuthProviderView()(requestWithToken, config, messages)
  }

  "UnauthorisedAuthProviderView" must {
    "display the correct headings and titles" in new ViewFixture {
      validateTitle("unauthorised.authprovider.title")
      doc.getElementsByTag("h1").text mustBe messages("unauthorised.authprovider.title")
    }

    "display the correct body content" in new ViewFixture {
      doc.getElementsByClass("govuk-body").text() must include(messages("unauthorised.authprovider.content"))
    }

    "display the login button linking to sign out then sign in" in new ViewFixture {
      val button: Element = doc.getElementById("button")

      button.text() mustBe messages("unauthorised.authprovider.button")
      button.attr("href") mustBe s"${config.logoutUrl}?continue=${config.loginContinue}"
    }
  }
}
