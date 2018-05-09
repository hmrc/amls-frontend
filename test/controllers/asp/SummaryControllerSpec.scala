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

package controllers.asp

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.asp.Asp
import models.businessmatching.TrustAndCompanyServices
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}
import play.api.test.Helpers._
import services.businessmatching.{NextService, ServiceFlow}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val serviceFlow = mock[ServiceFlow]
    val controller = new SummaryController(mockCacheConnector, serviceFlow, mockStatusService, self.authConnector)

    mockCacheSave[Asp]

    when {
      mockStatusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(true)

    when {
      serviceFlow.inNewServiceFlow(any())(any(), any(), any())
    } thenReturn Future.successful(false)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = Asp(None, None)

      mockCacheFetch[Asp](Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      mockCacheFetch[Asp](None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "Post" must {
    "load the Asp model and set hasAccepted to true" in new Fixture {
      val postRequest = request.withFormUrlEncodedBody()

      val model = Asp(None, None)
      mockCacheFetch(Some(model))

      val result = controller.post()(postRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

      verify(mockCacheConnector).save[Asp](eqTo(Asp.key), eqTo(model.copy(hasAccepted = true)))(any(), any(), any())
    }

    "redirect to the New Service Information controller" when {
      "status is Approved and this service has just been added" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody()

        mockApplicationStatus(SubmissionDecisionApproved)

        when {
          mockStatusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        when {
          serviceFlow.inNewServiceFlow(any())(any(), any(), any())
        } thenReturn Future.successful(true)

        val model = Asp(None, None)
        mockCacheFetch(Some(model))

        val result = controller.post()(postRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.add.routes.NewServiceInformationController.get().url)
      }
    }

  }
}
