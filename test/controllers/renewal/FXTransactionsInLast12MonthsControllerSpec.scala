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
import models.businessmatching._
import models.renewal.{FXTransactionsInLast12Months, Renewal}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class FXTransactionsInLast12MonthsControllerSpec extends AmlsSpec with MockitoSugar  {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new FXTransactionsInLast12MonthsController (
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

    val cacheMap = mock[CacheMap]
    when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(cacheMap)))

    def setupBusinessMatching(activities: Set[BusinessActivity]) = when {
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(BusinessMatching(activities = Some(BusinessActivities(activities))))
  }

  val emptyCache = CacheMap("", Map.empty)

  "RenewalForeignExchangeTransactionsController" must {

    "load the page 'How many foreign exchange transactions'" in new Fixture {

      when(controller.dataCacheConnector.fetch[Renewal](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("renewal.msb.fx.transactions.expected.title"))
    }

    "load the page 'How many foreign exchange transactions' with pre populated data" in new Fixture  {

      when(controller.dataCacheConnector.fetch[Renewal](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Renewal(
        fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("12345678963"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("12345678963")

    }

    "Show error message when user has not filled the mandatory fields" in new Fixture  {

      val newRequest = request.withFormUrlEncodedBody(
        "fxTransaction" -> ""
      )

      when {
        cacheMap.getEntry[Renewal](Renewal.key)
      } thenReturn Some(Renewal())

      when(mockRenewalService.updateRenewal(any())(any(),any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.renewal.fx.transactions.in.12months"))

    }

    trait FlowFixture extends Fixture {
      val newRequest = request.withFormUrlEncodedBody (
        "fxTransaction" -> "12345678963"
      )

      when {
        cacheMap.getEntry[Renewal](Renewal.key)
      } thenReturn Some(Renewal())

      when(mockRenewalService.updateRenewal(any())(any(),any(), any()))
              .thenReturn(Future.successful(mock[CacheMap]))
    }

    "Successfully save data in save4later and navigate to Next page" when {

      "business activities does not contain HVD or ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness))
        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }

      "business activities contains HVD" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing))
        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideUKController.get().url))
      }

      "business activities contains ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, AccountancyServices))
        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }

      "business activities contains HVD and ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing, AccountancyServices))
        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get().url))
      }
    }

    "Successfully save data in save4later and navigate to Summary page in edit mode" when {
      "business activities does not contain HVD or ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness))
        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }

      "business activities contains HVD" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing))
        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }

      "business activities contains ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, AccountancyServices))
        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }

      "business activities contains HVD and ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing, AccountancyServices))
        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }
    }

  }
}
