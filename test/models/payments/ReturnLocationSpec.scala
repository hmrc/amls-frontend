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

import org.scalatestplus.play.OneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import utils.GenericTestHelper

class ReturnLocationSpec extends GenericTestHelper with OneAppPerSuite {


  "The ReturnLocation model" must {

    "correctly determine the absolute url based on the current request" when {
      "request is running on localhost" in {

        val call = controllers.routes.ConfirmationController.paymentConfirmation("reference")
        val model = ReturnLocation(call)

        model.absoluteUrl mustBe s"https://somehost:9000${call.url}"

      }

    }
  }
  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.amls-frontend.public.host" -> "somehost:9000")
    .configure("microservice.services.amls-frontend.public.secure" -> true)
    .build()
}
