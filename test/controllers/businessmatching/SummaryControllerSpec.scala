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

import cats.data.OptionT
import cats.implicits._
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.BusinessType.LPrLLP
import models.businessmatching._
import models.businessmatching.updateservice._
import models.status.{SubmissionDecisionApproved, SubmissionReady}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

  override lazy val app = GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.business-matching-variation" -> false)
    .build()

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks{

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

    def mockGetModel(model: Option[BusinessMatching]) = when {
      controller.businessMatchingService.getModel(any(),any(),any())
    } thenReturn {
      if(model.isDefined){
        OptionT.some[Future,BusinessMatching](model)
      } else {
        OptionT.none[Future, BusinessMatching]
      }
    }

    def mockUpdateModel = when {
      controller.businessMatchingService.updateModel(any())(any(),any(),any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)

    def mockCommit = when {
      controller.businessMatchingService.commitVariationData(any(),any(),any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)
  }

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCache must be(DataCacheConnector)
    }

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

    "hide the edit links when not in pre-approved status" in new Fixture {
      val model = businessMatchingWithTypesGen(Some(LPrLLP)).sample.get

      mockGetModel(Some(model))
      mockApplicationStatus(SubmissionDecisionApproved)

      val result = controller.get()(request)
      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.select("a.change-answer").size mustBe 0
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
          mockCommit
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

class SummaryControllerWithVariationSpec extends GenericTestHelper with BusinessMatchingGenerator with DependencyMocks {

  override lazy val app = GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.business-matching-variation" -> true)
    .build()

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks{

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

    def mockGetModel(model: Option[BusinessMatching]) = when {
      controller.businessMatchingService.getModel(any(),any(),any())
    } thenReturn {
      if(model.isDefined){
        OptionT.some[Future,BusinessMatching](model)
      } else {
        OptionT.none[Future, BusinessMatching]
      }
    }

    def mockUpdateModel = when {
      controller.businessMatchingService.updateModel(any())(any(),any(),any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)

    def mockCommit = when {
      controller.businessMatchingService.commitVariationData(any(),any(),any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)
  }

  "Get" must {

    "show the edit links when not in pre-submission status" in new Fixture {
      val model = businessMatchingWithTypesGen(Some(LPrLLP)).sample.get

      mockGetModel(Some(model))
      mockApplicationStatus(SubmissionDecisionApproved)

      val result = controller.get()(request)
      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.select("a.change-answer").size mustBe 2
    }
  }

  "redirect to TradingPremisesController" when {
    "status is post-submission" when {
      "UpdateService is not complete" which {
        "updates the hasAccepted flag on the model" in new Fixture {

          val model = businessMatchingGen.sample.get.activities(
            BusinessActivities(
              Set(HighValueDealing),
              Some(Set(BillPaymentServices))
            )
          )
          val postRequest = request.withFormUrlEncodedBody()

          mockGetModel(Some(model))
          mockUpdateModel
          mockCommit
          mockCacheFetch[UpdateService](Some(UpdateService()), Some(UpdateService.key))
          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.post()(postRequest)

          status(result) mustBe SEE_OTHER

          redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.routes.TradingPremisesController.get(0).url)
        }
      }
      "UpdateService is not defined" which {
        "updates the hasAccepted flag on the model" in new Fixture {

          val model = businessMatchingGen.sample.get.activities(
            BusinessActivities(
              Set(HighValueDealing),
              Some(Set(BillPaymentServices))
            )
          )
          val postRequest = request.withFormUrlEncodedBody()

          mockGetModel(Some(model))
          mockUpdateModel
          mockCommit
          mockCacheFetch[UpdateService](None, Some(UpdateService.key))
          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.post()(postRequest)

          status(result) mustBe SEE_OTHER

          redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.routes.TradingPremisesController.get(0).url)
        }
      }
    }
  }

  "redirect to RegistrationProgressController" when {
    "UpdateService is complete" which {
      "updates the hasAccepted flag on the model" in new Fixture {

        val model = businessMatchingGen.sample.get.activities(
          BusinessActivities(
            Set(HighValueDealing),
            Some(Set(BillPaymentServices))
          )
        )
        val postRequest = request.withFormUrlEncodedBody()

        mockGetModel(Some(model))
        mockUpdateModel
        mockCommit
        mockCacheFetch[UpdateService](Some(UpdateService(
          Some(NewActivitiesAtTradingPremisesNo),
          Some(TradingPremisesActivities(Set(1))),
          Some(SubmittedActivitiesAtTradingPremisesNo),
          Some(TradingPremisesActivities(Set(1)))
        )), Some(UpdateService.key))

        val result = controller.post()(postRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
      }
    }
  }

}