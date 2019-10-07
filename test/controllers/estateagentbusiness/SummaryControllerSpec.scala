/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.estateagentbusiness.EstateAgentBusiness
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SummaryController(
      mock[DataCacheConnector],
      SuccessfulAuthAction, ds = commonDependencies,
      mock[StatusService],
      mock[ServiceFlow],
      cc = mockMcc)

    val model = EstateAgentBusiness(None, None)

    when {
      controller.statusService.isPreSubmission(Some(any()), any(), any())(any(),any())
    } thenReturn Future.successful(false)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {
      when(controller.dataCache.fetch[EstateAgentBusiness](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      when(controller.dataCache.fetch[EstateAgentBusiness](any(), any())(any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "post is called" must {
    "redirect to the bank account details page" when {

      "all questions are complete" in new Fixture {

        val emptyCache = CacheMap("", Map.empty)

        val newRequest = request.withFormUrlEncodedBody( "hasAccepted" -> "true")

        when(controller.dataCache.fetch[EstateAgentBusiness](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(model.copy(hasAccepted = false))))

        when(controller.dataCache.save[EstateAgentBusiness](any(), meq(EstateAgentBusiness.key), any())( any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }

    }
  }
}
