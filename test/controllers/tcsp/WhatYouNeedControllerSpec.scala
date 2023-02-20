/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.tcsp

import controllers.actions.SuccessfulAuthAction
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}
import views.html.tcsp.what_you_need

class WhatYouNeedControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks{
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[what_you_need]
    val controller = new WhatYouNeedController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      what_you_need = view)
  }

  "WhatYouNeedController" when {

    "get is called" must {

      "respond with SEE_OTHER and redirect to the 'what you need' page" in new Fixture {

        val result = controller.get()(request)
        status(result) must be(OK)
      }
    }
  }
}
