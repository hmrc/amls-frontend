/*
 * Copyright 2022 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.cash_payment

import scala.concurrent.Future


class CashPaymentOverTenThousandEurosControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks{
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[cash_payment]
    val controller = new CashPaymentController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      cash_payment = view)
  }

  val emptyCache = CacheMap("", Map.empty)

  "CashPaymentOverTenThousandEurosController" must {

    "on GET" must {

      "load the Cash Payment Over Ten Thousand Euros page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("hvd.cash.payment.title") + " - " + Messages("summary.hvd") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
      }

      "load Yes when Cash payment from mongoCache returns True" in new Fixture {
        // scalastyle:off magic.number
        val firstDate = Some(CashPaymentFirstDate(new LocalDate(1990, 2, 24)))
        val activities = Hvd(cashPayment = Some(CashPayment(CashPaymentOverTenThousandEuros(true), firstDate)))

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("acceptedAnyPayment-true").attr("checked") mustBe "checked"
        htmlValue.getElementById("acceptedAnyPayment-true").attr("checked") mustBe "checked"

      }

      "load No when cashPayment from mongoCache returns No" in new Fixture {

        val cashPayment = Some(CashPayment(CashPaymentOverTenThousandEuros(false), None))
        val activities = Hvd(cashPayment = cashPayment)

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("acceptedAnyPayment-false").attr("checked") mustBe "checked"

      }
    }

    "on POST" must {

      "successfully redirect to the Date of First Cash Payment page on selection of 'Yes' when edit mode is on" in new Fixture {

        val newRequest = requestWithUrlEncodedBody("acceptedAnyPayment" -> "true",
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
        redirectLocation(result) must be(Some(controllers.hvd.routes.CashPaymentFirstDateController.get(true).url))
      }

      "successfully redirect to the Date of First Cash Payment page on selection of 'Yes' when edit mode is off" in new Fixture {

        val newRequest = requestWithUrlEncodedBody("acceptedAnyPayment" -> "true",
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
        redirectLocation(result) must be(Some(controllers.hvd.routes.CashPaymentFirstDateController.get().url))
      }

      "successfully redirect to the Linked Cash Payments page on selection of 'No' when edit mode is off" in new Fixture {
        val newRequest = requestWithUrlEncodedBody("acceptedAnyPayment" -> "false")

        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.LinkedCashPaymentsController.get().url))
      }

      "successfully redirect to the Summary page on selection of Option 'No' when edit mode is on" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "acceptedAnyPayment" -> "false"
        )
        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
      }

      "show invalid data error" in new Fixture {

        val newRequest = requestWithUrlEncodedBody("" -> "")
        when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.hvd.accepted.cash.payment"))
      }
    }
  }
}