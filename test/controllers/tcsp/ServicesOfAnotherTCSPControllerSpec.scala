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

import generators.AmlsReferenceNumberGenerator
import models.tcsp.{ServicesOfAnotherTCSPYes, Tcsp}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class ServicesOfAnotherTCSPControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with AmlsReferenceNumberGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new ServicesOfAnotherTCSPController (
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector
    )
  }

  "ServicesOfAnotherTCSPController" when {

    "get is called" must {

      "display the Does your business use the services of another Trust or Company Service Provider page" in new Fixture {

        mockCacheFetch[Tcsp](None)

        val result = controller.get()(request)
        status(result) must be(OK)

      }

      "display the the Does your business use the services of another Trust or Company Service Provider page with pre populated data" in new Fixture {

        mockCacheFetch[Tcsp](Some(Tcsp(doesServicesOfAnotherTCSP = Some(true))))

        val result = controller.get()(request)
        status(result) must be(OK)

      }

    }

    "post is called" when {

      "valid data" must {

        "redirect to SummaryController" when {

          "edit is true" when {
            "further action is not required in AnotherTCSPSupervisionController" in new Fixture {

              mockCacheFetch[Tcsp](Some(Tcsp(servicesOfAnotherTCSP = Some(ServicesOfAnotherTCSPYes(amlsRegistrationNumber)))))
              mockCacheSave[Tcsp]

              val newRequest = request.withFormUrlEncodedBody(
                "servicesOfAnotherTCSP" -> "true"
              )

              val result = controller.post(true)(newRequest)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.SummaryController.get().url))
            }
          }

          "edit is false" when {
            "request equals false" in new Fixture {

              mockCacheFetch[Tcsp](None)
              mockCacheSave[Tcsp]

              val newRequest = request.withFormUrlEncodedBody(
                "servicesOfAnotherTCSP" -> "false"
              )

              val result = controller.post()(newRequest)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.SummaryController.get().url))
            }
          }

        }

        "redirect to AnotherTCSPSupervisionController" when {

          "edit is false" in new Fixture {

            mockCacheFetch[Tcsp](None)
            mockCacheSave[Tcsp]

            val newRequest = request.withFormUrlEncodedBody(
              "servicesOfAnotherTCSP" -> "true"
            )

            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AnotherTCSPSupervisionController.get().url))
          }

          "edit is true" when {
            "servicesOfAnotherTCSP is changed from false to true" in new Fixture {

              mockCacheFetch[Tcsp](None)
              mockCacheSave[Tcsp]

              val newRequest = request.withFormUrlEncodedBody(
                "servicesOfAnotherTCSP" -> "true"
              )

              val result = controller.post()(newRequest)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.AnotherTCSPSupervisionController.get().url))
            }
          }

        }

      }

      "respond with BAD_REQUEST" when {
        "invalid data" in new Fixture {

          val newRequestInvalid = request.withFormUrlEncodedBody(
            "servicesOfAnotherTCSP" -> ""
          )

          val result = controller.post()(newRequestInvalid)
          status(result) must be(BAD_REQUEST)
        }
      }

    }

  }

  it must {

    "remove data from ServicesOfAnotherTCSP" when {
      "request is edit from true to false" in new Fixture {

        mockCacheFetch[Tcsp](Some(Tcsp(doesServicesOfAnotherTCSP = Some(true), servicesOfAnotherTCSP = Some(ServicesOfAnotherTCSPYes(amlsRegistrationNumber)))))
        mockCacheSave[Tcsp]

        val newRequest = request.withFormUrlEncodedBody(
          "servicesOfAnotherTCSP" -> "false"
        )

        val result = controller.post(true)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))

        verify(controller.dataCacheConnector).save(any(),eqTo(Tcsp(doesServicesOfAnotherTCSP = Some(false), hasChanged = true)))(any(),any(),any())

      }
    }

  }

}
