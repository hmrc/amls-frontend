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

import connectors.DataCacheConnector
import models.estateagentbusiness.{EstateAgentBusiness, PenalisedUnderEstateAgentsActYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class PenalisedUnderEstateAgentsActControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new PenalisedUnderEstateAgentsActController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PenalisedUnderEstateAgentsActController" must {

    "load the blank page when the user visits the first time" in new Fixture {
      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("estateagentbusiness.penalisedunderestateagentsact.title"))
    }

    "load the page with data when the user revisits at a later time" in new Fixture {
      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(EstateAgentBusiness(None, None, None,
        Some(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before"))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[name=penalisedUnderEstateAgentsAct]").`val` must be("true")
      document.select("textarea[name=penalisedUnderEstateAgentsActDetails]").`val` must be("Do not remember why penalised before")
    }


    "on post capture the details provided by the user for penalised before" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "penalisedUnderEstateAgentsAct" -> "true",
        "penalisedUnderEstateAgentsActDetails" -> "Do not remember why penalised before"
      )

      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.PenalisedByProfessionalController.get().url))
    }


    "on post with missing data remain on the same page and also retain the data supplied" in new Fixture {

      val requestWithIncompleteData = request.withFormUrlEncodedBody(
        "penalisedUnderEstateAgentsActDetails" -> "Do not remember why penalised before"
      )

      val result = controller.post()(requestWithIncompleteData)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("textarea[name=penalisedUnderEstateAgentsActDetails]").`val` must be("Do not remember why penalised before")
      document.select("a[href=#penalisedUnderEstateAgentsAct]").html() must include(Messages("error.required.eab.penalised.under.act"))

    }
  }

}