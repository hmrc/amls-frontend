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

import org.jsoup.nodes.Element
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.Html
import utils.AmlsViewSpec
import views.Fixture
import views.html.deregister.DeregistrationConfirmationView

class DeregistrationConfirmationViewSpec extends AmlsViewSpec {

  "render view" in new Fixture {

    val deregistrationConfirmationView                    = inject[DeregistrationConfirmationView]
    implicit val request: Request[AnyContentAsEmpty.type] = addTokenForView()

    def view: Html                                        = deregistrationConfirmationView("01", "Acme Production LTD", "XBML00000567890")

    val content: String = doc.text()
    content must include("You have deregistered")
    content must include("Acme Production LTD")
    content must include("Your reference number")
    content must include("XBML00000567890")
    content must include(
      "You have deregistered from your supervision with HMRC under the Anti-Money Laundering Supervision."
    )
    content must include(
      "Your given reason for doing so is: Business is out of scope as no longer carrying out activities covered by the Money Laundering Regulations"
    )
    content must include("You can re-register for supervision from your registration page.")
    content must include("Print this page")

    val changeLink: Element = doc.getElementById("landing-page")

    changeLink.text() mustBe "registration page"
    changeLink.attr("href") mustBe controllers.routes.LandingController.get().url
  }

}
