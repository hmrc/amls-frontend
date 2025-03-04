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

import models.deregister.DeregistrationReason
import org.jsoup.nodes.Element
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.Html
import utils.AmlsViewSpec
import views.Fixture
import views.html.deregister.DeregistrationCheckYourAnswersView

class DeregistrationCheckYourAnswersViewSpec extends AmlsViewSpec {

  "render view" in new Fixture {

    val deregistrationCheckYourAnswersView                = inject[DeregistrationCheckYourAnswersView]
    implicit val request: Request[AnyContentAsEmpty.type] = addTokenForView()
    def view: Html                                        = deregistrationCheckYourAnswersView(DeregistrationReason.OutOfScope)

    val content: String = doc.text()
    content must include("Check your answers")
    content must include("Why are you deregistering the business?")
    content must include(
      "Business is out of scope as no longer carrying out activities covered by the Money Laundering Regulations"
    )
    val changeLink: Element = doc.getElementById("cya-change-link")

    changeLink.text() mustBe "Change"
    changeLink.attr("href") mustBe controllers.deregister.routes.DeregistrationReasonController.get.url
    val acceptAndContinue: Element = doc.getElementById("button")
    acceptAndContinue.text() mustBe "Accept and continue"
  }

}
