/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.businessdetails

import controllers.actions.SuccessfulAuthAction
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class WhatYouNeedControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new WhatYouNeedController (
      authAction = SuccessfulAuthAction
    )
  }

  "WhatYouNeedController" must {
    "get" must {

      "load the page" in new Fixture {

        val pageTitle = Messages("title.wyn") + " - " +
          Messages("summary.businessdetails") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        val result = controller.get(request)
        status(result) must be(OK)
        contentAsString(result) must include(pageTitle)
      }
    }
  }
}
