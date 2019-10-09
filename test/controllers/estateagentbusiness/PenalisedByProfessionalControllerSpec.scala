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
import models.estateagentbusiness._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future


class PenalisedByProfessionalControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures{

  trait Fixture {
    self => val request = addToken(authRequest)

    val controller = new PenalisedByProfessionalController (
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)
  }

  val emptyCache = CacheMap("", Map.empty)

  "PenalisedByProfessionalController" must {

    "on get display the Penalised By Professional Body page" in new Fixture {
      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any(), any())
        ( any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("estateagentbusiness.penalisedbyprofessional.title"))
    }


  "on get display the Penalised By Professional Body page with pre populated data" in new Fixture {

    when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any(), any())
      (any(), any())).thenReturn(Future.successful(Some(EstateAgentBusiness(None, None,Some(ProfessionalBodyYes("details"))))))

    val result = controller.get()(request)
    status(result) must be(OK)
    contentAsString(result) must include ("details")

  }

  "on post with valid data" in new Fixture {

    val newRequest = requestWithUrlEncodedBody(
      "penalised" -> "true",
      "professionalBody" -> "details"
    )

    when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any(), any())
      (any(), any())).thenReturn(Future.successful(None))

    when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any(), any())
      ( any(), any())).thenReturn(Future.successful(emptyCache))

    val result = controller.post()(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.SummaryController.get().url))
  }

  "on post with invalid data" in new Fixture {

    val newRequest = requestWithUrlEncodedBody(
      "penalisedYes" -> "details"
    )

    val result = controller.post()(newRequest)
    status(result) must be(BAD_REQUEST)

    val document = Jsoup.parse(contentAsString(result))

  }

   "on post with valid data in edit mode" in new Fixture {

     val newRequest = requestWithUrlEncodedBody(
       "penalised" -> "true",
      "professionalBody" -> "details"
     )

     when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any(), any())
       ( any(), any())).thenReturn(Future.successful(None))

     when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any(), any())
       (any(), any())).thenReturn(Future.successful(emptyCache))

     val result = controller.post(true)(newRequest)
     status(result) must be(SEE_OTHER)
     redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.SummaryController.get().url))
   }
  }
}


