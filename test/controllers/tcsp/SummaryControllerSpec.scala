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
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val defaultProvidedServices = ProvidedServices(Set(PhonecallHandling, Other("other service")))
    val defaultCompanyServiceProviders = TcspTypes(Set(RegisteredOfficeEtc,
      CompanyFormationAgent(true, false)))
    val defaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes("12345678")

    val model = Tcsp(
      Some(defaultCompanyServiceProviders),
      Some(defaultProvidedServices),
      Some(defaultServicesOfAnotherTCSP)
    )

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      when(controller.dataCache.fetch[Tcsp](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)

    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.dataCache.fetch[Tcsp](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "POST" must {

    "redirect to RegistrationProgressController" when {

      "the model has been updated with hasAccepted" in new Fixture {

        when {
          controller.dataCache.fetch[Tcsp](any())(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        when {
          controller.dataCache.save[Tcsp](any(),any())(any(),any(),any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        val result = controller.post()(request)

        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))

        verify(controller.dataCache).save[Tcsp](any(), eqTo(model.copy(hasAccepted = true)))(any(),any(),any())

      }
    }

  }
}
