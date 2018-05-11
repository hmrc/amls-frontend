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
import models.estateagentbusiness._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class ResidentialRedressSchemeControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ResidentialRedressSchemeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ResidentialRedressSchemeController" must {

    "use correct services" in new Fixture {
      PenalisedByProfessionalController.authConnector must be(AMLSAuthConnector)
      PenalisedByProfessionalController.dataCacheConnector must be(DataCacheConnector)
    }

    "on get load Residential Redress Scheme page" in new Fixture {
      when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("estateagentbusiness.registered.redress.title"))
    }
  }

  "on get load redress scheme page with pre populated data" in new Fixture {

    when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(EstateAgentBusiness(None, Some(Other("test")),None, None))))

    val result = controller.get()(request)
    status(result) must be(OK)
    contentAsString(result) must include ("test")

  }

  "on post with valid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "isRedress" -> "true",
      "propertyRedressScheme" -> "04",
      "other" -> "test"
    )

    when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

    when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

    val result = controller.post()(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.PenalisedUnderEstateAgentsActController.get().url))
  }

  "on post with invalid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "isRedress" -> "true",
      "propertyRedressScheme" -> "04"
    )

    val result = controller.post()(newRequest)
    status(result) must be(BAD_REQUEST)

    val document = Jsoup.parse(contentAsString(result))
    contentAsString(result) must include("This field is required")
  }

   "on post with valid data in edit mode" in new Fixture {

     val newRequest = request.withFormUrlEncodedBody(
       "isRedress" -> "true",
       "propertyRedressScheme" -> "01"
     )

     when(controller.dataCacheConnector.fetch[EstateAgentBusiness](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

     when(controller.dataCacheConnector.save[EstateAgentBusiness](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

     val result = controller.post(true)(newRequest)
     status(result) must be(SEE_OTHER)
     redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.SummaryController.get().url))
   }
}


