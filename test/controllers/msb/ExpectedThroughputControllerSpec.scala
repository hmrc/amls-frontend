/*
 * Copyright 2020 HM Revenue & Customs
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
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.moneyservicebusiness.{ExpectedThroughput, MoneyServiceBusiness}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.msb.expected_throughput

import scala.concurrent.Future

class ExpectedThroughputControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[expected_throughput]
    val controller = new ExpectedThroughputController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mockStatusService,
      serviceFlow = mockServiceFlow,
      cc = mockMcc,
      expected_throughput = view)

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExpectedThroughputController" must {

    "on get display the Throughput Expected In next 12Months page" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      mockApplicationStatus(NotCompleted)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.throughput.title"))
    }

    "on get display the Expected throughput page with pre populated data" in new Fixture {

      mockApplicationStatus(NotCompleted)

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(Some(ExpectedThroughput.First)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "on get display the Throughput Expected In next 12Months page when approved and the service has just been added" in new Fixture {
      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      mockApplicationStatus(SubmissionDecisionApproved)

      mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.throughput.title"))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.msb.throughput"))
    }

    "on post with valid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "throughput" -> "01"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.BranchesOrAgentsController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "throughput" -> "01"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }
  }
}
