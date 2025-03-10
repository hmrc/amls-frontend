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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.PercentageFormProvider
import models.renewal.{PercentageOfCashPaymentOver15000, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.PercentageView

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000ControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[Cache]

    val emptyCache = Cache.empty

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService     = mock[RenewalService]
    lazy val view                   = inject[PercentageView]
    val controller                  = new PercentageOfCashPaymentOver15000Controller(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[PercentageFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  "PercentageOfCashPaymentOver15000Controller" when {

    "get is called" must {

      "display the Percentage Of CashPayment Over 15000 page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(
          "What percentage of your turnover came from cash payments of €10,000 or more?"
        )
      }

      "display the Percentage Of CashPayment Over 15000 page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any(), any())(any())).thenReturn(
          Future.successful(
            Some(Renewal(percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First)))
          )
        )

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=zeroToTwenty]").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST when given invalid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.PercentageOfCashPaymentOver15000Controller.post().url)
          .withFormUrlEncodedBody(
          )

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.required.renewal.hvd.percentage"))
      }

      "when edit is false" must {
        "redirect to the CashPaymentController" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PercentageOfCashPaymentOver15000Controller.post().url)
            .withFormUrlEncodedBody(
              "percentage" -> PercentageOfCashPaymentOver15000.First.toString
            )

          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

          when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.renewal.routes.CashPaymentsCustomersNotMetController.get().url)
          )
        }
      }

      "when edit is true" must {
        "redirect to the SummaryController" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PercentageOfCashPaymentOver15000Controller.post().url)
            .withFormUrlEncodedBody(
              "percentage" -> PercentageOfCashPaymentOver15000.First.toString
            )

          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

          when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
        }
      }
    }
  }
}
