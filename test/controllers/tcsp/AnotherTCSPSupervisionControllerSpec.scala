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
import forms.tcsp.AnotherTCSPSupervisionFormProvider
import models.tcsp.{ServicesOfAnotherTCSPYes, Tcsp}
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.tcsp.AnotherTCSPSupervisionView

class AnotherTCSPSupervisionControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[AnotherTCSPSupervisionView]
    val controller = new AnotherTCSPSupervisionController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      cc = mockMcc,
      formProvider = inject[AnotherTCSPSupervisionFormProvider],
      view = view,
      error = errorView
    )
  }

  "AnotherTCSPSupervisionController" when {

    "get is called" must {

      "display another_tcsp_supervision view without pre-filled input" in new Fixture {

        mockCacheFetch[Tcsp](None)

        val result = controller.get()(request)
        status(result) must be(OK)

      }

      "display another_tcsp_supervision view with pre-filled input" in new Fixture {

        val mlrRefNumber = "12345678"

        mockCacheFetch[Tcsp](Some(Tcsp(servicesOfAnotherTCSP = Some(ServicesOfAnotherTCSPYes(Some(mlrRefNumber))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val content = Jsoup.parse(contentAsString(result))
        content.getElementById("mlrRefNumber").`val`() must be(mlrRefNumber)
      }

    }

    "post is called" must {

      "redirect to SummaryController" when {

        "valid data" when {
          "edit is false" in new Fixture {

            mockCacheFetch[Tcsp](None)
            mockCacheSave[Tcsp]

            val newRequest = FakeRequest(POST, routes.AnotherTCSPSupervisionController.post().url)
              .withFormUrlEncodedBody(
                "servicesOfAnotherTCSP" -> "true",
                "mlrRefNumber"          -> "12345678"
              )

            val result = controller.post()(newRequest)

            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }

          "edit is true" in new Fixture {

            mockCacheFetch[Tcsp](None)
            mockCacheSave[Tcsp]

            val newRequest = FakeRequest(POST, routes.AnotherTCSPSupervisionController.post(true).url)
              .withFormUrlEncodedBody(
                "servicesOfAnotherTCSP" -> "true",
                "mlrRefNumber"          -> "12345678"
              )

            val result = controller.post(true)(newRequest)

            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }
        }
      }

    }

  }

}
