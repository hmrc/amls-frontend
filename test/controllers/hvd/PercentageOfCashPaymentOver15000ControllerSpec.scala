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

package controllers.hvd

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.HighValueDealing
import models.businessmatching.updateservice.ServiceChangeRegister
import models.hvd.{Hvd, PercentageOfCashPaymentOver15000}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000ControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val controller = new PercentageOfCashPaymentOver15000Controller(SuccessfulAuthAction,
                                                                    mockCacheConnector,
                                                                    mockServiceFlow,
                                                                    mockStatusService)

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = CacheMap("", Map.empty)

  "PercentageOfCashPaymentOver15000Controller" must {

    "on get display the Percentage Of CashPayment Over 15000 page" in new Fixture {
      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("hvd.percentage.title"))
    }

    "on get display the Percentage Of CashPayment Over 15000 page with pre populated data" in new Fixture {
      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(Hvd(percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "continue to show the correct view" when {
      "application is in variation mode but the service has just been added" in new Fixture {
        when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        mockIsNewActivityNewAuth(true, Some(HighValueDealing))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("hvd.percentage.title"))
      }
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
      )

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.hvd.percentage"))
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "percentage" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "percentage" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))
    }
  }
}
