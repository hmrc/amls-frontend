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

package controllers.bankdetails

import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import play.api.test.Helpers._

class DoYouHaveABankAccountControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)
    val controller = new DoYouHaveABankAccountController(self.authConnector)

  }

  "The GET action" should {
    "return an OK response" in new Fixture {
      status(controller.get()(request)) mustBe OK
    }
  }

  "The POST action" should {
    "redirect to the 'bank name' page" when {
      "'yes' is selected" in new Fixture {
        val formData = "hasBankAccount" -> "true"
        val result = controller.post()(request.withFormUrlEncodedBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.bankdetails.routes.BankAccountNameController.get(1).url)
      }

      "'no' is selected" in new Fixture {
        val formData = "hasBankAccount" -> "false"
        val result = controller.post()(request.withFormUrlEncodedBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.bankdetails.routes.YourBankAccountsController.get().url)
      }

      "nothing is selected" in new Fixture {
        val result = controller.post()(request)

        status(result) mustBe BAD_REQUEST
      }
    }

  }

}
