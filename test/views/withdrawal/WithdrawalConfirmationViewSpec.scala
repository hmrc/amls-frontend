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

package views.withdrawal

import models.withdrawal.WithdrawalReason
import org.jsoup.nodes.Element
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.Html
import utils.AmlsViewSpec
import views.Fixture
import views.html.withdrawal.WithdrawalConfirmationView

class WithdrawalConfirmationViewSpec extends AmlsViewSpec {

  "render view" in new Fixture {

    val deregistrationConfirmationView = inject[WithdrawalConfirmationView]
    implicit val request: Request[AnyContentAsEmpty.type] = addTokenForView()
    def view: Html = deregistrationConfirmationView("Acme Production LTD", "XBML00000567890", WithdrawalReason.OutOfScope)

    val content: String = doc.text()
    content must include("Application withdrawn")
    content must include("Acme Production LTD")
    content must include("Your reference number")
    content must include("XBML00000567890")
    content must include("You have withdrawn your application for registration with HMRC under the Anti-Money Laundering Supervision.")
    content must include("Your given reason for doing so is: Business is out of scope as no longer carrying out activities covered by the Money Laundering Regulations.")
    content must include("If your business carries out any activities covered by the Money Laundering regulations, you will need to be registered with an appropriate supervisory body.")
    content must include("You can register for supervision from your registration page.")
    content must include("Print this page")

    val changeLink: Element = doc.getElementById("landing-page")

    changeLink.text() mustBe "registration page"
    changeLink.attr("href") mustBe controllers.routes.LandingController.get().url
  }

}
