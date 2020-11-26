/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import models.tcsp._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.tcsp.service_provider_types

import scala.concurrent.Future

class TcspTypesControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(self.authRequest)

    val cache = mock[DataCacheConnector]
    lazy val view = app.injector.instanceOf[service_provider_types]
    lazy val controller = new TcspTypesController(
      cache,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service_provider_types = view)

    val defaultProvidedServices = ProvidedServices(Set(PhonecallHandling, Other("other service")))
    val defaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes("12345678")

    val defaultCompanyServiceProviders = TcspTypes(Set(RegisteredOfficeEtc,
      CompanyFormationAgent, NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc))

    val model = Tcsp(
      Some(defaultCompanyServiceProviders),
      Some(OnlyOffTheShelfCompsSoldYes),
      Some(ComplexCorpStructureCreationNo),
      Some(defaultProvidedServices),
      Some(true),
      Some(defaultServicesOfAnotherTCSP)
    )

    val cacheMap = CacheMap("", Map.empty)

    when(cache.fetch[Tcsp](any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(model)))

    when(cache.save[Tcsp](any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "TcspTypesController" must {

    "Get is called" must {

      "load the what kind of Tcsp are you page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any(), any())).thenReturn(Future.successful(None))


        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "load the Kind of Tcsp are you page with pre-populated data" in new Fixture {

        val tcspTypes = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc))

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any(), any())).thenReturn(Future.successful(Some(Tcsp(Some(tcspTypes)))))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=01]").hasAttr("checked") must be(true)
        document.select("input[value=02]").hasAttr("checked") must be(true)
        document.select("input[value=04]").hasAttr("checked") must be(true)
      }
    }

    "Post" must {

      "successfully navigate to Which services does your business provide? page when the option Registered office is selected" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "serviceProviders[0]" -> "01",
          "serviceProviders[1]" -> "02",
          "serviceProviders[2]" -> "03"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any(), any())(any(), any())).thenReturn(Future.successful(cacheMap))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tcsp.routes.ProvidedServicesController.get().url))

      }

      "successfully navigate to services of another tcsp page when other than Registered office option is selected " in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "serviceProviders[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any(), any())(any(), any())).thenReturn(Future.successful(cacheMap))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tcsp.routes.ServicesOfAnotherTCSPController.get().url))
      }

      "successfully clear out the data"  when {
        "full model present in mongo and removing CompanyFormationAgent and RegisteredOfficeEtc" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "serviceProviders[0]" -> "01",
            "serviceProviders[1]" -> "02",
            "serviceProviders[2]" -> "04"
          )

          val expectedModel = Tcsp(Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc))),
            None, None, None, Some(true), Some(ServicesOfAnotherTCSPYes("12345678")), true, false)

          val result = controller.post()(newRequest)
          status(result) mustBe SEE_OTHER
          verify(controller.dataCacheConnector).save[Tcsp](any(), any(), eqTo(expectedModel))(any(), any())
        }
      }

      "successfully navigate to next page while storing data in in mongoCache in edit mode" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "serviceProviders[]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any(), any())(any(), any())).thenReturn(Future.successful(cacheMap))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tcsp.routes.SummaryController.get().url))
      }

    }

    "respond with BAD_REQUEST" when {

      "throw error an invalid data entry" in new Fixture {
        val newrequest = requestWithUrlEncodedBody(
          "serviceProviders[]" -> "06"
        )

        val result = controller.post()(newrequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }
}
