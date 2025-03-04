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

import controllers.actions.SuccessfulAuthAction
import forms.renewal.FXTransactionsInLast12MonthsFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.renewal.{FXTransactionsInLast12Months, Renewal}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.FXTransactionsInLast12MonthsView

import scala.concurrent.Future

class FXTransactionsInLast12MonthsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    lazy val mockRenewalService = mock[RenewalService]
    lazy val view               = inject[FXTransactionsInLast12MonthsView]
    val controller              = new FXTransactionsInLast12MonthsController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[FXTransactionsInLast12MonthsFormProvider],
      view = view
    )

    val cacheMap = mock[Cache]

    when(mockRenewalService.fetchAndUpdateRenewal(any(), any())).thenReturn(Future.successful(Some(cacheMap)))

    when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

    def setupBusinessMatching(activities: Set[BusinessActivity]) = when {
      mockRenewalService.getBusinessMatching(any())
    } thenReturn Future.successful(Some(BusinessMatching(activities = Some(BusinessActivities(activities)))))
  }

  val emptyCache = Cache.empty

  "RenewalForeignExchangeTransactionsController" must {

    "load the page 'How many foreign exchange transactions'" in new Fixture {

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("renewal.msb.fx.transactions.expected.title"))
    }

    "load the page 'How many foreign exchange transactions' with pre populated data" in new Fixture {

      when(mockRenewalService.getRenewal(any()))
        .thenReturn(
          Future.successful(
            Some(Renewal(fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("12345678963"))))
          )
        )

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include("12345678963")
    }

    "Show error message when user has not filled the mandatory fields" in new Fixture {

      val newRequest = FakeRequest(POST, routes.FXTransactionsInLast12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "fxTransaction" -> ""
        )

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.renewal.fx.transactions.in.12months"))
    }

    trait FlowFixture extends Fixture {
      val newRequest = FakeRequest(POST, routes.FXTransactionsInLast12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "fxTransaction" -> "12345678963"
        )
    }

    "return 500" when {

      "updating mongoCache fails" in new FlowFixture {
        when(mockRenewalService.fetchAndUpdateRenewal(any(), any())).thenReturn(Future.successful(None))

        setupBusinessMatching(activities = Set(MoneyServiceBusiness))
        val result = controller.post()(newRequest)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "Successfully save data in mongoCache and navigate to Next page" when {

      "business activities does not contain HVD or ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness))
        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
      }

      "business activities contains HVD" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing))
        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
      }

      "business activities contains ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, AccountancyServices))
        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
      }

      "business activities contains HVD and ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing, AccountancyServices))
        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
      }
    }

    "Successfully save data in mongoCache and navigate to Summary page in edit mode" when {
      "business activities does not contain HVD or ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness))
        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
      }

      "business activities contains HVD" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing))
        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
      }

      "business activities contains ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, AccountancyServices))
        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
      }

      "business activities contains HVD and ASP" in new FlowFixture {
        setupBusinessMatching(activities = Set(MoneyServiceBusiness, HighValueDealing, AccountancyServices))
        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
      }
    }
  }
}
