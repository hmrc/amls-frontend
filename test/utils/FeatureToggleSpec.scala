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

package utils

import controllers.BaseController
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.Action
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class FeatureToggleSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {



  trait TestController {
    self: BaseController =>
    def TestToggle:FeatureToggle
    lazy val get = TestToggle {
      Action {
        Ok("Success")
      }
    }
  }

  object ToggleOnController extends BaseController with TestController {
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val TestToggle = FeatureToggle(true)
  }

  object ToggleOffController extends BaseController with TestController {
    override protected val authConnector = mock[AuthConnector]
    override val TestToggle = FeatureToggle(false)
  }

  "ToggleOnController" must {
    "return the result of calling the inner action" in {
      val result = ToggleOnController.get(FakeRequest())
      status(result) mustBe 200
      contentAsString(result) mustBe "Success"
    }
  }

  "ToggleOffController" must {
    "return a `NotFound` result" in {
      val result = ToggleOffController.get(FakeRequest())
      status(result) mustBe NOT_FOUND
    }
  }
}
