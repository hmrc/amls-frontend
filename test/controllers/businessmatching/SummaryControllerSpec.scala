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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.BusinessType.LPrLLP
import models.businessmatching._
import models.businessmatching.updateservice._
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val controller = new SummaryController (
      dataCache = mockCacheConnector,
      authConnector = self.authConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService
    )

    when {
      mockStatusService.isPreSubmission(any())
    } thenReturn true

    when {
      mockStatusService.isPending(any())
    } thenReturn false

    mockApplicationStatus(NotCompleted)

    def mockGetModel(model: Option[BusinessMatching]) = when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn {
      if (model.isDefined) {
        OptionT.some[Future, BusinessMatching](model)
      } else {
        OptionT.none[Future, BusinessMatching]
      }
    }

    def mockUpdateModel = when {
      controller.businessMatchingService.updateModel(any())(any(), any(), any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {
      val model = BusinessMatching(
        activities = Some(BusinessActivities(Set.empty[BusinessActivity]))
      )

      mockGetModel(Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      mockGetModel(None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "hide the edit links when not in pre-approved status and is pending" in new Fixture {
      val model = businessMatchingWithTypesGen(Some(LPrLLP)).sample.get

      mockGetModel(Some(model))

      when {
        controller.statusService.isPreSubmission(any())
      } thenReturn false

      when {
        controller.statusService.isPending(any())
      } thenReturn true

      val result = controller.get()(request)
      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.select("a.change-answer").size mustBe 0
    }

    "show the 'Register Services' page when the user wants to change their services" in new Fixture {
      val model = BusinessMatching(
        activities = Some(BusinessActivities(Set(EstateAgentBusinessService)))
      )

      mockGetModel(Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)

      val doc = Jsoup.parse(contentAsString(result))
      val editUrl = doc.select("section.register-services a.change-answer").first().attr("href")

      editUrl mustBe controllers.businessmatching.routes.RegisterServicesController.get().url
    }
  }

  "Post" when {
    "called" must {
      "redirect to RegistrationProgressController" which {
        "updates the hasAccepted flag on the model" in new Fixture {

          val model = businessMatchingGen.sample.get.copy(hasAccepted = false)
          val postRequest = request.withFormUrlEncodedBody()

          mockGetModel(Some(model))
          mockUpdateModel
          mockCacheFetch[UpdateService](None)

          val result = controller.post()(postRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

          val captor = ArgumentCaptor.forClass(classOf[BusinessMatching])
          verify(mockBusinessMatchingService).updateModel(captor.capture())(any(), any(), any())
          captor.getValue.hasAccepted mustBe true
          captor.getValue.preAppComplete mustBe true
        }
      }

      "return Internal Server Error if the business matching model can't be updated" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody()

        mockGetModel(None)

        val result = controller.post()(postRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
