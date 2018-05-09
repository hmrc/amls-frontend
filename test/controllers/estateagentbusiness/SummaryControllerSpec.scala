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

package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.estateagentbusiness.EstateAgentBusiness
import models.tcsp.Tcsp
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SummaryController(mock[DataCacheConnector], self.authConnector, mock[StatusService], mock[ServiceFlow])

    val model = EstateAgentBusiness(None, None)

    when {
      controller.statusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(false)

    when {
      controller.serviceFlow.inNewServiceFlow(any())(any(), any(), any())
    } thenReturn Future.successful(false)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {
      when(controller.dataCache.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      when(controller.dataCache.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "post is called" must {
    "redirect to the bank account details page" when {

      "all questions are complete" in new Fixture {

        val emptyCache = CacheMap("", Map.empty)

        val newRequest = request.withFormUrlEncodedBody( "hasAccepted" -> "true")

        when(controller.dataCache.fetch[EstateAgentBusiness](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(model.copy(hasAccepted = false))))

        when(controller.dataCache.save[EstateAgentBusiness](meq(EstateAgentBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }

    }

    "redirect to NewServiceInformationController" when {
      "status is not pre-submission and activity has just been added" in new Fixture {
        val cache = mock[CacheMap]

        when {
          controller.dataCache.fetch[EstateAgentBusiness](any())(any(),any(),any())
        } thenReturn Future.successful(Some(model))

        when {
          controller.dataCache.save[EstateAgentBusiness](any(), any())(any(),any(),any())
        } thenReturn Future.successful(cache)

        when {
          controller.serviceFlow.inNewServiceFlow(any())(any(), any(), any())
        } thenReturn Future.successful(true)

        when {
          controller.statusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        val result = controller.post()(request)

        redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.add.routes.NewServiceInformationController.get().url)

      }
    }
  }
}
