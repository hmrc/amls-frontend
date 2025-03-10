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
import forms.hvd.LinkedCashPaymentsFormProvider
import models.hvd.{Hvd, LinkedCashPayments}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.LinkedCashPaymentsView

import scala.concurrent.Future

class LinkedCashPaymentsControllerSpec extends AmlsSpec with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[LinkedCashPaymentsView]
    val controller = new LinkedCashPaymentsController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[LinkedCashPaymentsFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  "LinkedCashPaymentsController" must {

    "successfully load UI for the first time" in new Fixture {
      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
        .thenReturn(Future.successful(None))

      val title  = messages("hvd.identify.linked.cash.payment.title") + " - " +
        messages("summary.hvd") + " - " +
        messages("title.amls") + " - " + messages("title.gov")
      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe title
    }

    "successfully load UI from mongoCache" in new Fixture {

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
        .thenReturn(Future.successful(Some(Hvd(linkedCashPayment = Some(LinkedCashPayments(true))))))

      val title  = messages("hvd.identify.linked.cash.payment.title") + " - " +
        messages("summary.hvd") + " - " +
        messages("title.amls") + " - " + messages("title.gov")
      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe title
      htmlValue.getElementById("linkedCashPayments").`val`() mustBe "true"
      htmlValue.getElementById("linkedCashPayments-2").`val`() mustBe "false"
    }

    "successfully redirect to nex page when submitted with valida data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.LinkedCashPaymentsController.post().url)
        .withFormUrlEncodedBody("linkedCashPayments" -> "true")

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.ReceiveCashPaymentsController.get().url))
    }

    "successfully redirect to nex page when submitted with valida data in edit mode" in new Fixture {

      val newRequest = FakeRequest(POST, routes.LinkedCashPaymentsController.post().url)
        .withFormUrlEncodedBody("linkedCashPayments" -> "false")

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
    }

    "fail with validation error when mandatory field is missing" in new Fixture {
      val newRequest = FakeRequest(POST, routes.LinkedCashPaymentsController.post().url)
        .withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.hvd.linked.cash.payment"))
    }

  }

}
