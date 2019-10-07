/*
 * Copyright 2019 HM Revenue & Customs
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
import models.hvd._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future


class CashPaymentFirstDateControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture  with DependencyMocks{
    self => val request = addToken(authRequest)
    val controller = new CashPaymentFirstDateController(mockCacheConnector, authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)
  }

  val emptyCache = CacheMap("", Map.empty)

  "CashPaymentFirstDateController" must {

    "on GET" must {

      "load the Date of First Payment page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("hvd.cash.payment.date.title") + " - " + Messages("summary.hvd") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
      }
    }

    "on POST" must {

      "successfully redirect to the Summary page when edit mode is on" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "paymentDate.day" -> "12",
          "paymentDate.month" -> "5",
          "paymentDate.year" -> "1999"
        )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))
      }

      "successfully redirect to the Linked Payments page when edit mode is off" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "paymentDate.day" -> "12",
          "paymentDate.month" -> "5",
          "paymentDate.year" -> "1999"
        )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.LinkedCashPaymentsController.get().url))
      }

      "show error if invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody()
        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))

      }

      "show error if missing day" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "acceptedAnyPayment" -> "true",
          "paymentDate.day" -> "",
          "paymentDate.month" -> "5",
          "paymentDate.year" -> "1999"
        )
        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))

      }

      "show error if year field too short" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("acceptedAnyPayment" -> "true",
          "paymentDate.day" -> "12",
          "paymentDate.month" -> "5",
          "paymentDate.year" -> "99"
        )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))
      }

      "show error if year field too long" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("acceptedAnyPayment" -> "true",
          "paymentDate.day" -> "12",
          "paymentDate.month" -> "5",
          "paymentDate.year" -> "19995"
        )

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.expected.jodadate.format"))
      }
    }
  }
}
