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

package controllers.tcsp

import connectors.DataCacheConnector
import models.tcsp._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class TcspTypesControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new TcspTypesController {
      override val dataCacheConnector:DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector :AuthConnector =  self.authConnector
    }
  }

  val cacheMap = CacheMap("", Map.empty)

  "TcspTypesController" must {

    "Get is called" must {

      "load the what kind of Tcsp are you page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "load the Kind of Tcsp are you page with pre-populated data" in new Fixture {

        val tcspTypes = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc))

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Tcsp(Some(tcspTypes)))))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=01]").hasAttr("checked") must be(true)
        document.select("input[value=02]").hasAttr("checked") must be(true)
        document.select("input[value=04]").hasAttr("checked") must be(true)
      }
    }

    "Post" must {

      "successfully navigate to Which services does your business provide? page when the option Registered office is selected" in  new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "serviceProviders[0]" -> "01",
          "serviceProviders[1]" -> "02",
          "serviceProviders[2]" -> "03"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(cacheMap))

        val result =  controller.post() (newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be (Some(controllers.tcsp.routes.ProvidedServicesController.get().url))

      }

      "successfully navigate to services of another tcsp page when other than Registered office option is selected " in  new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "serviceProviders[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(cacheMap))

        val result =  controller.post() (newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be (Some(controllers.tcsp.routes.ServicesOfAnotherTCSPController.get().url))

      }

      "successfully navigate to next page while storing data in in save4later in edit mode" in  new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "serviceProviders[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(cacheMap))

        val result =  controller.post(true) (newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be (Some(controllers.tcsp.routes.SummaryController.get().url))
      }

    }

    "respond with BAD_REQUEST" when {

      "throw error an invalid data entry" in  new Fixture {
        val newrequest = request.withFormUrlEncodedBody(
          "serviceProviders[]" -> "05",
          "onlyOffTheShelfCompsSold" -> "",
          "complexCorpStructureCreation" -> ""
        )

        val result =  controller.post() (newrequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }
}
