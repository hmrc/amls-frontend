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

package controllers.tcsp

import models.tcsp._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.businessmatching.ServiceFlow
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val defaultProvidedServices = ProvidedServices(Set(PhonecallHandling, Other("other service")))
    val defaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes("12345678")

    val defaultCompanyServiceProviders = TcspTypes(Set(RegisteredOfficeEtc,
      CompanyFormationAgent(true, false)))

    val model = Tcsp(
      tcspTypes = Some(defaultCompanyServiceProviders),
      providedServices = Some(defaultProvidedServices),
      doesServicesOfAnotherTCSP = Some(true),
      servicesOfAnotherTCSP = Some(defaultServicesOfAnotherTCSP)
    )

    val controller = new SummaryController(
      mockCacheConnector,
      self.authConnector,
      mock[ServiceFlow],
      mockStatusService
    )

    when {
      controller.statusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(false)
  }

  "sortProviders" must {

    "return Trust or company formation agent as the last item in the sorted list" when {

      "CompanyFormationAgent service provider is included" in new Fixture {

        val modelCopy: Tcsp = model.copy(
          tcspTypes=Some(
            TcspTypes(
              Set(
                CompanyFormationAgent(false,false),
                TrusteeProvider,
                RegisteredOfficeEtc,
                NomineeShareholdersProvider,
                CompanyDirectorEtc
              )
            )
          )
        )

        val res = controller.sortProviders(modelCopy)

        res mustEqual List(
          "Company director, secretary, or partner provider",
          "Nominee shareholders provider",
          "Registered office, business address, or virtual office services provider",
          "Trustee provider",
          "Trust or company formation agent"
        )
      }

    }

    "return empty list" when {

      "no service providers list is given" in new Fixture {
        val modelCopy: Tcsp = model.copy(
          tcspTypes=Some(
            TcspTypes(
              Set()
            )
          )
        )

        val res = controller.sortProviders(modelCopy)

        res mustEqual List()
      }
    }

    "return sorted list" when {

      "normal service provider list is provided" in new Fixture {
        val modelCopy: Tcsp = model.copy(
          tcspTypes=Some(
            TcspTypes(
              Set(TrusteeProvider,
                RegisteredOfficeEtc,
                NomineeShareholdersProvider,
                CompanyDirectorEtc
              )
            )
          )
        )

        val res = controller.sortProviders(modelCopy)

        res mustEqual List(
          "Company director, secretary, or partner provider",
          "Nominee shareholders provider",
          "Registered office, business address, or virtual office services provider",
          "Trustee provider"
        )
      }

    }

  }

  "Get" must {




    "load the summary page when section data is available" in new Fixture {

      mockCacheFetch[Tcsp](Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)

    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      mockCacheFetch[Tcsp](None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "POST" must {
    "redirect to RegistrationProgressController" when {
      "the model has been updated with hasAccepted" in new Fixture {

        mockCacheFetch[Tcsp](Some(model))
        mockCacheSave[Tcsp]

        val result = controller.post()(request)

        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))

        verify(controller.dataCache).save[Tcsp](any(), eqTo(model.copy(hasAccepted = true)))(any(),any(),any())

      }
    }
  }
}
