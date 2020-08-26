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

package controllers.bankdetails

import controllers.actions.SuccessfulAuthAction
import models.bankdetails.BankDetails
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.bankdetails.has_bank_account

class HasBankAccountControllerSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {
    self =>

    lazy val hasBankAcc = app.injector.instanceOf[has_bank_account]
    val request = addToken(authRequest)
    val controller = new HasBankAccountController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      mockMcc,
      hasBankAcc
    )

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
        val result = controller.post()(requestWithUrlEncodedBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.bankdetails.routes.BankAccountNameController.getNoIndex.url)
      }

      "'no' is selected" which {
        "also saves an empty list of bank details into the cache" in new Fixture {
          mockCacheSave(Seq.empty[BankDetails], Some(BankDetails.key))

          val formData = "hasBankAccount" -> "false"
          val result = controller.post()(requestWithUrlEncodedBody(formData))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.bankdetails.routes.YourBankAccountsController.get().url)
        }
      }
    }

    "return the view with a Bad Request status" when {
      "nothing is selected" in new Fixture {
        val result = controller.post()(request)

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
