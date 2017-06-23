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

package controllers.withdrawal

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse, WithdrawalReason}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Prop.Exception
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class WithdrawalReasonControllerSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> true)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    val dataCacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val controller = new WithdrawalReasonController(authConnector, dataCacheConnector, amlsConnector, authService, statusService)

    val amlsRegistrationNumber = "XA1234567890L"

    when {
      authService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(amlsRegistrationNumber.some)

    when {
      amlsConnector.withdraw(eqTo(amlsRegistrationNumber), any())(any(), any(), any())
    } thenReturn Future.successful(mock[WithdrawSubscriptionResponse])

  }


  "WithdrawalReasonController" when {

    "get is called" must {

      "display withdrawal_reasons view without data" in new TestFixture {

        when(controller.dataCacheConnector.fetch[WithdrawalReason](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("withdrawal.reason.title"))

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("withdrawalReason-01").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-02").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-03").hasAttr("checked") must be(false)
        document.getElementById("withdrawalReason-04").hasAttr("checked") must be(false)
        document.getElementById("specifyOtherReason").`val`() must be("")
      }

      "display the page with pre-populated data" when {

        val outOfScopeId = "withdrawalReason-01"
        val notTradingInOwnRightId = "withdrawalReason-02"
        val underAnotherSupervisorId = "withdrawalReason-03"
        val otherId = "withdrawalReason-04"

        val withdrawalReason = Map(
          outOfScopeId -> WithdrawalReason.OutOfScope,
          notTradingInOwnRightId -> WithdrawalReason.NotTradingInOwnRight,
          underAnotherSupervisorId -> WithdrawalReason.UnderAnotherSupervisor,
          otherId -> WithdrawalReason.Other("reason")
        )

        withdrawalReason.foreach {
          case (id, model) =>
            model.toString in new TestFixture {

              when(controller.dataCacheConnector.fetch[WithdrawalReason](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(model)))

              val result = controller.get()(request)
              status(result) must be(OK)

              val document = Jsoup.parse(contentAsString(result))

              document.getElementById(outOfScopeId).hasAttr("checked") must be(outOfScopeId.equals(id))
              document.getElementById(notTradingInOwnRightId).hasAttr("checked") must be(notTradingInOwnRightId.equals(id))
              document.getElementById(underAnotherSupervisorId).hasAttr("checked") must be(underAnotherSupervisorId.equals(id))
              document.getElementById(otherId).hasAttr("checked") must be(otherId.equals(id))
            }
        }

        "given a reason with other" in new TestFixture {

          when(controller.dataCacheConnector.fetch[WithdrawalReason](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(WithdrawalReason.Other("reason"))))

          val result = controller.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("specifyOtherReason").`val`() must be("reason")

        }

      }

    }

    "post is called" when {

      "given valid data" must {

        "go to landing controller" which {
          "follows sending a withdrawal to amls" in new TestFixture {

            val newRequest = request.withFormUrlEncodedBody(
              "withdrawalReason" -> "01"
            )

            val withdrawal = WithdrawSubscriptionRequest(
              WithdrawSubscriptionRequest.DefaultAckReference,
              LocalDate.now(),
              WithdrawalReason.OutOfScope
            )

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)

            verify(amlsConnector).withdraw(eqTo(amlsRegistrationNumber), any())(any(), any(), any())

            redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))

          }
        }

      }

      "given invalid data" must {
        "return with BAD_REQUEST" in new TestFixture {

          val newRequest = request.withFormUrlEncodedBody(
            "withdrawalReason" -> "20"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "unable to withdraw" must {
        "return InternalServerError" in new TestFixture {

          when {
            authService.amlsRegistrationNumber(any(), any(), any())
          } thenReturn Future.successful(None)

          val newRequest = request.withFormUrlEncodedBody(
            "withdrawalReason" -> "01"
          )

          val result = controller.post()(newRequest)
          status(result) must be(INTERNAL_SERVER_ERROR)

        }
      }

    }

  }

}

class WithdrawalReasonControllerToggleOffSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> false)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val authService = mock[AuthEnrolmentsService]
    val cacheConnector = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    lazy val controller = new WithdrawalReasonController(authConnector, cacheConnector, amlsConnector, authService, statusService)
  }

  "The WithdrawalReasonController" when {
    "the GET method is called" must {
      "return 404 not found" in new TestFixture {
        status(controller.get(request)) mustBe NOT_FOUND
      }
    }
  }
}