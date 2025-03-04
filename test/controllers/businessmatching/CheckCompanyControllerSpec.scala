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

package controllers.businessmatching

import controllers.actions.SuccessfulAuthAction
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import utils.AmlsSpec
import views.html.businessmatching.CheckCompanyIsNotRegisteredView

class CheckCompanyControllerSpec extends AmlsSpec with ScalaFutures with Injecting with BeforeAndAfterEach {

  lazy val controller = new CheckCompanyController(
    SuccessfulAuthAction,
    commonDependencies,
    mockMcc,
    inject[CheckCompanyIsNotRegisteredView]
  )

  val request = FakeRequest()

  "CheckCompanyController" when {

    "get is called" must {

      "render the correct view" in {

        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(messages("businessmatching.checkbusiness.title"))
      }
    }

    "post is called" must {

      "redirect to the correct location" in {

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
      }
    }
  }
}
