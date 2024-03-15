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

package controllers.responsiblepeople

import controllers.actions.SuccessfulAuthAction
import org.scalatest.concurrent.ScalaFutures

import play.api.test.Helpers._
import utils.AmlsSpec
import views.html.responsiblepeople.WhoMustRegisterView

class WhoMustRegisterControllerSpec extends AmlsSpec  with ScalaFutures {

  trait Fixture {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[WhoMustRegisterView]
    val controller = new WhoMustRegisterController (
      authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc,
      view = view)
  }
  "WhoMustRegisterController" must {

      "load the page" in new Fixture {
        val result = controller.get(1)(request)
        status(result) must be(OK)
        contentAsString(result) must include(messages("responsiblepeople.whomustregister.ymr"))
      }
    }
}
