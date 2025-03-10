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

package controllers.hvd

import controllers.actions.SuccessfulAuthAction
import forms.hvd.PercentagePaymentFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.businessmatching.updateservice.ServiceChangeRegister
import models.hvd.PercentageOfCashPaymentOver15000.First
import models.hvd.{Hvd, PercentageOfCashPaymentOver15000}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.PercentageView

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000ControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val view  = inject[PercentageView]
    val controller =
      new PercentageOfCashPaymentOver15000Controller(
        SuccessfulAuthAction,
        ds = commonDependencies,
        mockCacheConnector,
        mockServiceFlow,
        mockStatusService,
        cc = mockMcc,
        formProvider = inject[PercentagePaymentFormProvider],
        view = view
      )

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = Cache.empty

  "PercentageOfCashPaymentOver15000Controller" must {

    "on get display the Percentage Of CashPayment Over 15000 page" in new Fixture {
      when(
        controller.statusService
          .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
      )
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("hvd.percentage.title"))
    }

    "on get display the Percentage Of CashPayment Over 15000 page with pre populated data" in new Fixture {
      when(
        controller.statusService
          .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
      )
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any())).thenReturn(
        Future.successful(Some(Hvd(percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First))))
      )

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select(s"input[value=${First.toString}]").hasAttr("checked") must be(true)
    }

    "continue to show the correct view" when {
      "application is in variation mode but the service has just been added" in new Fixture {
        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        )
          .thenReturn(Future.successful(NotCompleted))

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        mockIsNewActivityNewAuth(true, Some(HighValueDealing))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("hvd.percentage.title"))
      }
    }

    "on post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PercentageOfCashPaymentOver15000Controller.post().url)
        .withFormUrlEncodedBody()

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.hvd.percentage"))
    }

    "on post with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PercentageOfCashPaymentOver15000Controller.post().url)
        .withFormUrlEncodedBody(
          "percentage" -> PercentageOfCashPaymentOver15000.First.toString
        )

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PercentageOfCashPaymentOver15000Controller.post().url)
        .withFormUrlEncodedBody(
          "percentage" -> PercentageOfCashPaymentOver15000.First.toString
        )

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
    }
  }
}
