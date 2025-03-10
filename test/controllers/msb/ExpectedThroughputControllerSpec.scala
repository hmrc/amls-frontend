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

package controllers.msb

import controllers.actions.SuccessfulAuthAction
import forms.msb.ExpectedThroughputFormProvider
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.moneyservicebusiness.ExpectedThroughput.First
import models.moneyservicebusiness.MoneyServiceBusiness
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.ExpectedThroughputView

import scala.concurrent.Future

class ExpectedThroughputControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[ExpectedThroughputView]
    val controller = new ExpectedThroughputController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mockStatusService,
      serviceFlow = mockServiceFlow,
      cc = mockMcc,
      formProvider = inject[ExpectedThroughputFormProvider],
      view = view
    )

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = Cache.empty

  "ExpectedThroughputController" must {

    "on get display the Throughput Expected In next 12 Months page" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      mockApplicationStatus(NotCompleted)

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("msb.throughput.title"))
    }

    "on get display the Expected throughput page with pre populated data" in new Fixture {

      mockApplicationStatus(NotCompleted)

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(Some(MoneyServiceBusiness(Some(First)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select(s"input[value=${First.toString}]").hasAttr("checked") must be(true)
    }

    "on get display the Throughput Expected In next 12 Months page when approved and the service has just been added" in new Fixture {
      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      mockApplicationStatus(SubmissionDecisionApproved)

      mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("msb.throughput.title"))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ExpectedThroughputController.post().url)
        .withFormUrlEncodedBody(
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.msb.throughput"))
    }

    "on post with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ExpectedThroughputController.post().url)
        .withFormUrlEncodedBody(
          "throughput" -> First.toString
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.BranchesOrAgentsController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ExpectedThroughputController.post().url)
        .withFormUrlEncodedBody(
          "throughput" -> First.toString
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get.url))
    }
  }
}
