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
import forms.hvd.CashPaymentFirstDateFormProvider
import models.hvd._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.CashPaymentFirstDateView

import scala.concurrent.Future

class CashPaymentFirstDateControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[CashPaymentFirstDateView]
    val controller = new CashPaymentFirstDateController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[CashPaymentFirstDateFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  "CashPaymentFirstDateController" must {

    "on GET" must {

      "load the Date of First Payment page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe messages("hvd.cash.payment.date.title") + " - " + messages(
          "summary.hvd"
        ) + " - " + messages("title.amls") + " - " + messages("title.gov")
      }
    }

    "on POST" must {

      "successfully redirect to the Summary page when edit mode is on" in new Fixture {

        val newRequest = FakeRequest(POST, routes.CashPaymentFirstDateController.post().url)
          .withFormUrlEncodedBody(
            "paymentDate.day"   -> "12",
            "paymentDate.month" -> "5",
            "paymentDate.year"  -> "1999"
          )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
      }

      "successfully redirect to the Linked Payments page when edit mode is off" in new Fixture {

        val newRequest = FakeRequest(POST, routes.CashPaymentFirstDateController.post().url)
          .withFormUrlEncodedBody(
            "paymentDate.day"   -> "12",
            "paymentDate.month" -> "5",
            "paymentDate.year"  -> "1999"
          )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.LinkedCashPaymentsController.get().url))
      }

      "show error if invalid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.CashPaymentFirstDateController.post().url)
          .withFormUrlEncodedBody("paymentDate.day" -> "", "paymentDate.month" -> "", "paymentDate.year" -> "")
        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.date.hvd.all"))

      }

      "show error if missing day" in new Fixture {
        val newRequest = FakeRequest(POST, routes.CashPaymentFirstDateController.post().url)
          .withFormUrlEncodedBody(
            "paymentDate.day"   -> "",
            "paymentDate.month" -> "5",
            "paymentDate.year"  -> "1999"
          )
        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.date.hvd.one", "day"))

      }

      "show error if year field is in past" in new Fixture {

        val newRequest = FakeRequest(POST, routes.CashPaymentFirstDateController.post().url)
          .withFormUrlEncodedBody(
            "paymentDate.day"   -> "12",
            "paymentDate.month" -> "5",
            "paymentDate.year"  -> "1222"
          )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.date.hvd.past"))
      }

      "show error if in the future" in new Fixture {

        val newRequest = FakeRequest(POST, routes.CashPaymentFirstDateController.post().url)
          .withFormUrlEncodedBody(
            "paymentDate.day"   -> "12",
            "paymentDate.month" -> "5",
            "paymentDate.year"  -> "2200"
          )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.date.hvd.future"))
      }
    }
  }
}
