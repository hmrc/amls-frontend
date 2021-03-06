/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.withdrawal

import cats.implicits._
import connectors.AmlsConnector
import controllers.actions.SuccessfulAuthAction
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse, WithdrawalReason}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.withdrawal.withdrawal_reason

import scala.concurrent.Future

class WithdrawalReasonControllerSpec extends AmlsSpec {

  trait TestFixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    val statusService = mock[StatusService]
    lazy val view = app.injector.instanceOf[withdrawal_reason]
    lazy val controller = new WithdrawalReasonController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      amlsConnector,
      authService,
      statusService,
      mockCacheConnector,
      cc = mockMcc,
      withdrawal_reason = view)

    val amlsRegistrationNumber = "XA1234567890L"

    when {
      authService.amlsRegistrationNumber(Some(any()), Some(any()))(any(), any())
    } thenReturn Future.successful(amlsRegistrationNumber.some)

    when {
      amlsConnector.withdraw(eqTo(amlsRegistrationNumber), any(), any())(any(), any())
    } thenReturn Future.successful(mock[WithdrawSubscriptionResponse])
  }

  "WithdrawalReasonController" when {

    "get is called" must {

      "display withdrawal_reasons view without data" in new TestFixture {

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("withdrawal.reason.heading"))

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawalReason-01").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-02").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-03").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-04").hasAttr("checked") must be(false)
        document.getElementById("specifyOtherReason").`val`() must be("")
      }

    }

    "post is called" when {

      "given valid data" must {

        "go to landing controller" which {
          "follows sending a withdrawal to amls" when {
            "withdrawalReason is selection without other reason" in new TestFixture {
              val newRequest = requestWithUrlEncodedBody(
                "withdrawalReason" -> "01"
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[WithdrawSubscriptionRequest])
              verify(amlsConnector).withdraw(eqTo(amlsRegistrationNumber), captor.capture(), any())(any(), any())

              captor.getValue.withdrawalReason mustBe WithdrawalReason.OutOfScope

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
            }

            "withdrawalReason is selection with other reason" in new TestFixture {

              val newRequest = requestWithUrlEncodedBody(
                "withdrawalReason" -> "04",
                "specifyOtherReason" -> "reason"
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[WithdrawSubscriptionRequest])
              verify(amlsConnector).withdraw(eqTo(amlsRegistrationNumber), captor.capture(), any())(any(), any())

              captor.getValue.withdrawalReason mustBe WithdrawalReason.Other("reason")
              captor.getValue.withdrawalReasonOthers mustBe "reason".some

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))

            }
          }
        }
      }

      "given invalid data" must {
        "return with BAD_REQUEST" in new TestFixture {

          val newRequest = requestWithUrlEncodedBody(
            "withdrawalReason" -> "20"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "unable to withdraw" must {
        "return InternalServerError" in new TestFixture {

          when {
            authService.amlsRegistrationNumber(Some(any()), Some(any()))(any(), any())
          } thenReturn Future.successful(None)

          val newRequest = requestWithUrlEncodedBody(
            "withdrawalReason" -> "01"
          )

          val result = controller.post()(newRequest)
          status(result) must be(INTERNAL_SERVER_ERROR)

        }
      }

    }

  }

}
