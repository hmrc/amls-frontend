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
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.actions.SuccessfulAuthAction
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse, WithdrawalReason}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.Injecting
import services.AuthEnrolmentsService
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.withdrawal.WithdrawalCheckYourAnswersView

import scala.concurrent.Future

class WithdrawalCheckYourAnswersControllerSpec extends AmlsSpec with Injecting {

  trait TestFixture extends AuthorisedFixture {
    self =>

    val credentialId: String = SuccessfulAuthAction.credentialId
    val request              = addToken(authRequest)

    val amlsConnector: AmlsConnector                 = mock[AmlsConnector]
    val authEnrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
    val dataCacheConnector: DataCacheConnector       = mock[DataCacheConnector]

    lazy val view       = inject[WithdrawalCheckYourAnswersView]
    lazy val controller = new WithdrawalCheckYourAnswersController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = dataCacheConnector,
      amlsConnector = amlsConnector,
      authEnrolmentsService = authEnrolmentsService,
      cc = mockMcc,
      view = view
    )
  }

  "WithdrawalCheckYourAnswersController" when {

    "get is called" must {
      "display the Check Your Answers page" in new TestFixture {
        when(dataCacheConnector.fetch[WithdrawalReason](eqTo(credentialId), eqTo(WithdrawalReason.key))(any()))
          .thenReturn(Future.successful(Some(WithdrawalReason.OutOfScope)))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        private val content: String = contentAsString(result)
        content must include("Your registration")

        val document: Document = Jsoup.parse(content)
        document.select("h1").text mustBe "Check your answers"
      }
    }

    "post is called" must {
      "submit withdrawal and redirect to the landing page" in new TestFixture {

        when(dataCacheConnector.fetch[WithdrawalReason](eqTo(credentialId), eqTo(WithdrawalReason.key))(any()))
          .thenReturn(Future.successful(Some(WithdrawalReason.OutOfScope)))

        val amlsRegistrationNumber: String = "XA1234567890L"

        when {
          authEnrolmentsService.amlsRegistrationNumber(Some(any()), Some(any()))(any(), any())
        } thenReturn Future.successful(amlsRegistrationNumber.some)

        when {
          amlsConnector.withdraw(eqTo(amlsRegistrationNumber), any(), any())(any(), any())
        } thenReturn Future.successful(mock[WithdrawSubscriptionResponse])

        val result: Future[Result] = controller.post()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result).value mustBe controllers.withdrawal.routes.WithdrawalConfirmationController.get.url

        val captor: ArgumentCaptor[WithdrawSubscriptionRequest] =
          ArgumentCaptor.forClass(classOf[WithdrawSubscriptionRequest])

        verify(amlsConnector).withdraw(
          amlsRegistrationNumber = eqTo(amlsRegistrationNumber),
          request = captor.capture(),
          accountTypeId = any()
        )(any(), any())

        captor.getValue.withdrawalReason mustBe WithdrawalReason.OutOfScope
      }
    }
  }
}
