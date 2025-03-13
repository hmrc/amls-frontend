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

package controllers.tcsp

import controllers.actions.SuccessfulAuthAction
import forms.tcsp.ProvidedServicesFormProvider
import models.tcsp.ProvidedServices.{Other, PhonecallHandling}
import models.tcsp.{ProvidedServices, Tcsp}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.tcsp.ProvidedServicesView

import scala.concurrent.Future

class ProvidedServicesControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[ProvidedServicesView]
    val controller = new ProvidedServicesController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[ProvidedServicesFormProvider],
      view = view,
      error = errorView
    )
  }

  "ProvidedServicesController" must {

    "get" must {

      "load the provided services page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "load the provided services page with existing data" in new Fixture {

        val tcsp = Tcsp(providedServices = Some(ProvidedServices(Set(Other("some other service")))))
        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any())) thenReturn Future.successful(Some(tcsp))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[id=services_7]").hasAttr("checked") mustBe true
        document.select("input[name=details]").`val` must be("some other service")
      }
    }

    "post" must {

      val cacheMap = mock[Cache]

      "successfully navigate to next page when valid data is sent" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ProvidedServicesController.post().url)
          .withFormUrlEncodedBody(
            "services[0]" -> PhonecallHandling.toString
          )

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any(), any())(any()))
          .thenReturn(Future.successful(cacheMap))

        val result = controller.post()(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tcsp.routes.ServicesOfAnotherTCSPController.get().url))

      }

      "successfully navigate to summary page when valid data is sent and edit mode is on" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ProvidedServicesController.post(true).url)
          .withFormUrlEncodedBody(
            "services[0]" -> PhonecallHandling.toString
          )

        when(controller.dataCacheConnector.fetch[Tcsp](any(), any())(any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[Tcsp](any(), any(), any())(any()))
          .thenReturn(Future.successful(cacheMap))

        val result = controller.post(true)(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tcsp.routes.SummaryController.get().url))

      }

      "show an error when no option been selected" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ProvidedServicesController.post().url)
          .withFormUrlEncodedBody("" -> "")

        val result = controller.post()(newRequest)

        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("services-error").text must include(
          messages("error.required.tcsp.provided_services.services")
        )
      }

      "show an error when other option been selected and not providing the mandatory data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ProvidedServicesController.post(true).url)
          .withFormUrlEncodedBody(
            "services[0]" -> Other("").toString,
            "details"     -> ""
          )

        val result = controller.post(true)(newRequest)

        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("details-error").text must include(
          messages("error.required.tcsp.provided_services.details")
        )
      }
    }
  }
}
