/*
 * Copyright 2018 HM Revenue & Customs
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
import models.renewal.{PercentageOfCashPaymentOver15000, Renewal}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000ControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new PercentageOfCashPaymentOver15000Controller(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "PercentageOfCashPaymentOver15000Controller" when {

    "get is called" must {

      "display the Percentage Of CashPayment Over 15000 page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("hvd.percentage.title"))
      }

      "display the Percentage Of CashPayment Over 15000 page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Renewal(percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First)))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=01]").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST when given invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.renewal.hvd.percentage"))
      }

      "when edit is false" must {
        "redirect to the CashPaymentController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "percentage" -> "01"
          )

          when(mockRenewalService.getRenewal(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.renewal.routes.ReceiveCashPaymentsController.get().url))
        }
      }

      "when edit is true" must {
        "redirect to the SummaryController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "percentage" -> "01"
          )

          when(mockRenewalService.getRenewal(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
        }
      }
    }
  }
}
