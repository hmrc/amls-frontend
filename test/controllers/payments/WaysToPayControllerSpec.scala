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

package controllers.payments

import models.payments.WaysToPay
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

class WaysToPayControllerSpec extends PlaySpec with MockitoSugar with GenericTestHelper {

  trait Fixture extends AuthorisedFixture { self =>

    val request = addToken(authRequest)

    val controller = new WaysToPayController(
      authConnector = self.authConnector
    )

  }

  "WaysToPayController" when {

    "get is called" must {
      "return OK with view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("payments.waystopay.title"))

      }
    }

    "post is called" must {
      "redirect to TypeOfBankController" when {
        "enum is bacs" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> WaysToPay.Bacs.entryName
          )

          val result = controller.post()(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be (Some(controllers.payments.routes.TypeOfBankController.get().url))

        }
      }
    }

  }

}
