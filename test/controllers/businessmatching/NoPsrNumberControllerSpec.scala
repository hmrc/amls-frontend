/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.concurrent.ScalaFutures
import utils.{AuthorisedFixture, GenericTestHelper}
import play.api.test.Helpers._
import play.api.mvc.Results.Ok

class NoPsrNumberControllerSpec extends GenericTestHelper with ScalaFutures {

  trait Fixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    lazy val controller = new CannotContinueWithTheApplicationController {
      override protected def authConnector = self.authConnector
    }

  }

  "get" when {
    "called" must {
      "return an OK status" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK
      }
    }
  }

}
