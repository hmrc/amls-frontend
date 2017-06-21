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

package models.payments

import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.GenericTestHelper

class ReturnLocationSpec extends GenericTestHelper {

  "The ReturnLocation model" must {

    "correctly determine the absolute url based on the current request" when {

      "request is running on localhost" in {

        val call = controllers.routes.ConfirmationController.paymentConfirmation("reference")
        implicit val request = FakeRequest(GET, "http://localhost:9222/anti-money-laundering/confirmation")
        val model = ReturnLocation(call)

        model.returnUrl mustBe s"http://localhost:9222${call.url}"

      }

      "request is running in some other environment" in {

        val call = controllers.routes.ConfirmationController.paymentConfirmation("reference")
        implicit val request = FakeRequest(GET, "https://www.qa-environment.fake/anti-money-laundering")
        val model = ReturnLocation(call)

        model.returnUrl mustBe call.url

      }

    }
  }
}
