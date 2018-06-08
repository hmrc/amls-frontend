/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.bankdetails

import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture}
import views.TitleValidator

class WhatYouNeedControllerSpec extends AmlsSpec with ScalaFutures with TitleValidator {

  trait Fixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)
    val controller = new WhatYouNeedController(self.authConnector)
  }

  "WhatYouNeedController" when {

    "get is called" must {

      "respond with SEE_OTHER and redirect to the 'what you need' page" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)

        implicit val doc = Jsoup.parse(contentAsString(result))
        validateTitle(s"${Messages("title.wyn")} - ${Messages("summary.bankdetails")}")

        contentAsString(result) must include(Messages("button.continue"))
      }
    }
  }
}
