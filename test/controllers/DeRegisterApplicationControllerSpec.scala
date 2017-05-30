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

package controllers

import org.scalatest.MustMatchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

class DeRegisterApplicationControllerSpec extends GenericTestHelper with MustMatchers with OneAppPerSuite {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    val controller = new DeRegisterApplicationController(self.authConnector, messages)
  }

  "The DeRegisterApplicationController" when {
    "GET is called" must {
      "show the correct page" in new TestFixture {
        val result = controller.get()(request)
        status(result) mustBe OK
        contentAsString(result) must include(Messages("status.deregister.title"))
      }
    }
  }

}
