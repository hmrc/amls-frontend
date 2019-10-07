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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.renewal.{CETransactionsInLast12Months, Renewal}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class CETransactionsInLast12MonthsControllerSpec extends AmlsSpec with MockitoSugar  {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new CETransactionsInLast12MonthsController (
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      renewalService = mockRenewalService, cc = mockMcc
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "MsbCurrencyExchangeTransactionsController" must {

    "load the page 'How many currency exchange transactions'" in new Fixture {

      when(controller.dataCacheConnector.fetch[Renewal](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("renewal.msb.ce.transactions.expected.title"))
    }

    "load the page 'How many currency exchange transactions' with pre populated data" in new Fixture  {

      when(controller.dataCacheConnector.fetch[Renewal](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(Renewal(
        ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("12345678963"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("12345678963")

    }

    "Show error message when user has not filled the mandatory fields" in new Fixture  {

      val newRequest = request.withFormUrlEncodedBody(
        "ceTransaction" -> ""
      )

      when(controller.dataCacheConnector.fetch[Renewal](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(mockRenewalService.updateRenewal(any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.renewal.ce.transactions.in.12months"))

    }

    "Successfully save data in mongoCache and navigate to Next page" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody (
        "ceTransaction" -> "12345678963"
      )

      when(controller.dataCacheConnector.fetch[Renewal](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(mockRenewalService.updateRenewal(any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.renewal.routes.WhichCurrenciesController.get().url))
    }

    "Successfully save data in mongoCache and navigate to Summary page in edit mode" in new Fixture {

      val incomingModel = Renewal(
      )

      val outgoingModel = incomingModel.copy(
        ceTransactionsInLast12Months = Some(
          CETransactionsInLast12Months("12345678963")
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody (
        "ceTransaction" -> "12345678963"
      )

      when(controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))
        (any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(mockRenewalService.updateRenewal(any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
    }

  }
}
