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
import models.ReadStatusResponse
import models.businesscustomer.ReviewDetails
import models.registrationdetails.RegistrationDetails
import models.status.SubmissionReadyForReview
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.withdrawal.WithdrawApplicationView

import java.time.LocalDateTime
import scala.concurrent.Future

class WithdrawApplicationControllerSpec extends AmlsSpec {

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request        = addToken(authRequest)
    val amlsConnector  = mock[AmlsConnector]
    val cacheConnector = mock[DataCacheConnector]
    val statusService  = mock[StatusService]
    val enrolments     = mock[AuthEnrolmentsService]
    lazy val view      = app.injector.instanceOf[WithdrawApplicationView]
    val controller     = new WithdrawApplicationController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      amlsConnector,
      cacheConnector,
      enrolments,
      statusService,
      cc = mockMcc,
      view = view
    )

    val applicationReference = "SUIYD3274890384"
    val safeId               = "X87FUDIKJJKJH87364"
    val businessName         = "Business Name from registration details"
    val reviewDetails        = mock[ReviewDetails]

    // noinspection ScalaStyle
    val processingDate = LocalDateTime.of(2002, 1, 1, 12, 0, 0)
    val statusResponse = ReadStatusResponse(processingDate, "", None, None, None, None, renewalConFlag = false)

    when(reviewDetails.safeId).thenReturn(safeId)

    when {
      enrolments.amlsRegistrationNumber(Some(any()), Some(any))(any(), any())
    } thenReturn Future.successful(applicationReference.some)

    when {
      amlsConnector.registrationDetails(any(), eqTo(safeId))(any(), any())
    } thenReturn Future.successful(RegistrationDetails(businessName, isIndividual = false))

    when {
      statusService.getDetailedStatus(Some(any()), any(), any())(any(), any(), any())
    } thenReturn Future.successful((SubmissionReadyForReview, statusResponse.some))

    when {
      controller.statusService.getSafeIdFromReadStatus(any(), any(), any())(any(), any())
    } thenReturn Future.successful(Some(safeId))
  }

  "The WithdrawApplication controller" when {
    "the get method is called" must {
      "show the 'withdraw your application' page" in new TestFixture {
        val result = controller.get()(request)
        status(result) mustBe OK
      }

      "show the business name" in new TestFixture {
        val result = controller.get()(request)
        contentAsString(result) must include("Withdraw your application for Business Name from registration details")
      }
    }

    "go to WithdrawalReasonController" when {
      "the post method is called" in new TestFixture {
        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe routes.WithdrawalReasonController.get().url.some

      }
    }
  }
}
