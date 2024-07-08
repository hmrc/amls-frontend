/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.withdrawal.WithdrawalReasonFormProvider
import models.withdrawal.WithdrawalReason.{Other, OutOfScope}
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse, WithdrawalReason}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.AuthEnrolmentsService
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.withdrawal.WithdrawalReasonView

import scala.concurrent.Future

class WithdrawalReasonControllerSpec extends AmlsSpec with Injecting {

  trait TestFixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    lazy val view = inject[WithdrawalReasonView]
    lazy val controller = new WithdrawalReasonController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      amlsConnector,
      authService,
      cc = mockMcc,
      formProvider = inject[WithdrawalReasonFormProvider],
      view = view
    )

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

      "display the view without data" in new TestFixture {

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(messages("withdrawal.reason.heading"))

        val document = Jsoup.parse(contentAsString(result))

        WithdrawalReason.all.foreach { reason =>
          document.getElementById(reason.toString).hasAttr("checked") must be(false)
        }

        document.getElementById("specifyOtherReason").`val`() must be("")
      }
    }

    "post is called" when {

      "given valid data" must {

        "go to landing controller" which {
          "follows sending a withdrawal to amls" when {
            "withdrawalReason is selection without other reason" in new TestFixture {
              val newRequest = FakeRequest(POST, routes.WithdrawalReasonController.post().url).withFormUrlEncodedBody(
                "withdrawalReason" -> OutOfScope.toString
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[WithdrawSubscriptionRequest])
              verify(amlsConnector).withdraw(eqTo(amlsRegistrationNumber), captor.capture(), any())(any(), any())

              captor.getValue.withdrawalReason mustBe WithdrawalReason.OutOfScope

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
            }

            "withdrawalReason is selection with other reason" in new TestFixture {

              val newRequest = FakeRequest(POST, routes.WithdrawalReasonController.post().url).withFormUrlEncodedBody(
                "withdrawalReason" -> Other("").toString,
                "specifyOtherReason" -> "reason"
              )

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)

              val captor = ArgumentCaptor.forClass(classOf[WithdrawSubscriptionRequest])
              verify(amlsConnector).withdraw(eqTo(amlsRegistrationNumber), captor.capture(), any())(any(), any())

              captor.getValue.withdrawalReason mustBe Other("reason")
              captor.getValue.withdrawalReasonOthers mustBe "reason".some

              redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
            }
          }
        }
      }

      "given invalid data" must {
        "return with BAD_REQUEST" in new TestFixture {

          val newRequest = FakeRequest(POST, routes.WithdrawalReasonController.post().url).withFormUrlEncodedBody(
            "withdrawalReason" -> "foo"
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

          val newRequest = FakeRequest(POST, routes.WithdrawalReasonController.post().url).withFormUrlEncodedBody(
            "withdrawalReason" -> OutOfScope.toString
          )

          val result = controller.post()(newRequest)
          status(result) must be(INTERNAL_SERVER_ERROR)

        }
      }
    }
  }
}
