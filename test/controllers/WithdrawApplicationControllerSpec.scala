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

import connectors.AmlsConnector
import models.WithdrawSubscriptionResponse
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.AuthEnrolmentsService
import utils.{AuthorisedFixture, GenericTestHelper}
import cats.implicits._

import scala.concurrent.Future

class WithdrawApplicationControllerSpec extends GenericTestHelper {

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]

    lazy val controller = new WithdrawApplicationController(authConnector, amlsConnector, authService)

    val amlsRegistrationNumber = "XA1234567890L"

    when {
      authService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(amlsRegistrationNumber.some)

    when {
      amlsConnector.withdraw(eqTo(amlsRegistrationNumber))(any(), any(), any())
    } thenReturn Future.successful(WithdrawSubscriptionResponse("processing date"))
  }

  "The WithdrawApplication controller" must {
    "show the 'withdraw your application' page" when {
      "the get method is called" in new TestFixture {
        val result = controller.get()(request)
        status(result) mustBe OK
      }
    }

    "call the middle tier to initiate the withdrawal process" when {
      "the post method is called" in new TestFixture {
        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe controllers.routes.LandingController.get().url.some

        verify(amlsConnector).withdraw(eqTo(amlsRegistrationNumber))(any(), any(), any())
      }
    }

  }

}
