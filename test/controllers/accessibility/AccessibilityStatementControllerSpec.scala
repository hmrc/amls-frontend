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

package controllers.accessibility

import org.scalatest.mockito.MockitoSugar
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}

class AccessibilityStatementControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val controller = new AccessibilityStatementController(
      commonDependencies,
      mockMcc,
      appConfig,
      mock[BodyParsers.Default]
    )
  }

  "AccessibilityStatementController" must {

    "on get display accessibility statement" in new Fixture {

      val result = controller.get()(FakeRequest().withSession())
      println(contentAsString(result))
      status(result) mustBe OK
    }
  }
}
