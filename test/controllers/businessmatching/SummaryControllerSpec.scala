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

package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import generators.AmlsReferenceNumberGenerator
import generators.businesscustomer.ReviewDetailsGenerator
import generators.businessmatching.{BusinessActivitiesGenerator, BusinessMatchingGenerator}
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessMatching}
import models.businessmatching.BusinessType.Partnership
import models.businesscustomer.{Address, ReviewDetails}
import models.status.{SubmissionDecisionApproved, SubmissionReady}
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._
import services.StatusService
import models.Country
import models.businessmatching._
import models.businessmatching.BusinessType.LPrLLP
import play.api.inject.guice.GuiceApplicationBuilder
import services.businessmatching.BusinessMatchingService

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

  override lazy val app = GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.business-matching-variation" -> false)
    .build()

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val controller = new SummaryController {
      override val dataCache = mockCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mockStatusService
      override val businessMatchingService = mockBusinessMatchingService
    }

    when {
      controller.statusService.getStatus(any(), any(), any())
    } thenReturn Future.successful(SubmissionReady)
  }

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCache must be(DataCacheConnector)
    }

    "load the summary page when section data is available" in new Fixture {
      val model = BusinessMatching()

      mockCacheFetch[BusinessMatching](Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      mockCacheFetch[BusinessMatching](None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "hide the edit links when not in pre-approved status" in new Fixture {
      val model = businessMatchingWithTypesGen(Some(LPrLLP)).sample.get

      mockCacheFetch[BusinessMatching](Some(model))
      mockApplicationStatus(SubmissionDecisionApproved)

      val result = controller.get()(request)
      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.select("a.change-answer").size mustBe 0
    }
  }

  "Post" when {
    "called" must {
      "update the hasAccepted flag on the model" in new Fixture {
        val model = businessMatchingGen.sample.get.copy(hasAccepted = false)
        val postRequest = request.withFormUrlEncodedBody()

        mockCacheFetch[BusinessMatching](Some(model))
        mockCacheSave[BusinessMatching]

        val result = controller.post()(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
        verify(mockCacheConnector).save[BusinessMatching](any(), eqTo(model.copy(hasAccepted = true)))(any(), any(), any())
      }

      "return Internal Server Error if the business matching model can't be updated" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody()

        mockCacheFetch[BusinessMatching](None)

        val result = controller.post()(postRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
