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

package controllers.tcsp

import models.tcsp.{ServicesOfAnotherTCSPYes, Tcsp}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

class AnotherTCSPSupervisionControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new AnotherTCSPSupervisionController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector
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

        mockCacheFetch[Tcsp](Some(Tcsp(servicesOfAnotherTCSP = Some(ServicesOfAnotherTCSPYes(mlrRefNumber)))))

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

            val newRequest = request.withFormUrlEncodedBody(
              "servicesOfAnotherTCSP" -> "true",
              "mlrRefNumber" -> "12345678"
            )

            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }

          "edit is true" in new Fixture {

            mockCacheFetch[Tcsp](None)
            mockCacheSave[Tcsp]

            val newRequest = request.withFormUrlEncodedBody(
              "servicesOfAnotherTCSP" -> "true",
              "mlrRefNumber" -> "12345678"
            )

            val result = controller.post(true)(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }

        }

      }

      "respond with BAD_REQUEST" when {
        "invalid data" in new Fixture {

          val newRequestInvalid = request.withFormUrlEncodedBody(
            "servicesOfAnotherTCSP" -> "true",
            "mlrRefNumber" -> "adbg1233"
          )

          val result = controller.post()(newRequestInvalid)
          status(result) must be(BAD_REQUEST)
        }
      }

    }

  }

}
