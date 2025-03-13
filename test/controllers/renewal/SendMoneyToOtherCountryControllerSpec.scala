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

import cats.implicits._
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.SendMoneyToOtherCountryFormProvider
import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.renewal.{Renewal, SendMoneyToOtherCountry}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{RenewalService, StatusService}
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.SendMoneyToOtherCountryView

import scala.concurrent.Future

class SendMoneyToOtherCountryControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request  = addToken(authRequest)
    val cacheMap = mock[Cache]

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService      = mock[StatusService]
    lazy val mockRenewalService     = mock[RenewalService]

    lazy val view  = inject[SendMoneyToOtherCountryView]
    val controller = new SendMoneyToOtherCountryController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[SendMoneyToOtherCountryFormProvider],
      view = view
    )

    when {
      mockRenewalService.getRenewal(any())
    } thenReturn Future.successful(Renewal().some)

    when {
      mockRenewalService.updateRenewal(any(), any())
    } thenReturn Future.successful(cacheMap)

    when {
      mockDataCacheConnector.fetchAll(any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any())
    } thenReturn (Some(Renewal(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)))))

    def setupBusinessMatching(
      activities: Set[BusinessActivity] = Set(),
      msbServices: Set[BusinessMatchingMsbService] = Set()
    ) = when {
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(
      BusinessMatching(
        msbServices = Some(BusinessMatchingMsbServices(msbServices)),
        activities = Some(BusinessActivities(activities))
      )
    )
  }

  val emptyCache = Cache.empty

  "SendMoneyToOtherCountryController" must {

    "load the page 'Did you send money to other countries in the past 12 months?'" in new Fixture {
      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("renewal.msb.send.money.title"))
    }

    "load the page 'Do you send money to other countries?' with pre populated data" in new Fixture {
      when {
        mockRenewalService.getRenewal(any())
      } thenReturn Future.successful(Renewal(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))).some)

      val result   = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result)          must be(OK)
      contentAsString(result) must include(messages("renewal.msb.send.money.title"))
      document.select("input[name=money][checked]").`val` mustEqual "true"
    }

    "Show error message when user has not filled the mandatory fields" in new Fixture {
      setupBusinessMatching(msbServices = Set(TransmittingMoney, CurrencyExchange))

      val result = controller.post()(
        FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody("" -> "")
      )
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.renewal.send.money"))
    }

    "throw exception when Msb services in Business Matching returns none" in new Fixture {
      val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
        "money" -> "false"
      )

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(None)

      a[Exception] must be thrownBy {
        ScalaFutures.whenReady(controller.post(true)(newRequest))(x => x)
      }
    }
  }

  "posting valid data" must {
    "redirect to the SendTheLargestAmountOfMoneyController" when {
      "post yes" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "true"
        )

        setupBusinessMatching(Set(HighValueDealing), Set(TransmittingMoney))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.renewal.routes.SendTheLargestAmountsOfMoneyController.get().url)
        )
      }
    }

    "redirect to the CETransactionsInLast12MonthsController" when {
      "post no and has currency exchange" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "false"
        )

        setupBusinessMatching(Set(HighValueDealing), Set(CurrencyExchange))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.renewal.routes.CETransactionsInLast12MonthsController.get().url)
        )
      }
    }

    "redirect to the CETransactionsInLast12MonthsController" when {
      "post no and has currency exchange and foreign exchange" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "false"
        )

        setupBusinessMatching(Set(HighValueDealing), Set(CurrencyExchange, ForeignExchange))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.renewal.routes.CETransactionsInLast12MonthsController.get().url)
        )
      }
    }

    "redirect to the CETransactionsInLast12MonthsController" when {
      "post no and has foreign exchange" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "false"
        )

        setupBusinessMatching(Set(HighValueDealing), Set(ForeignExchange))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.renewal.routes.FXTransactionsInLast12MonthsController.get().url)
        )
      }
    }

    "redirect to the CustomersOutsideIsUKController" when {
      "post no and has HVD and ASP and NOT CE" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "false"
        )

        setupBusinessMatching(Set(HighValueDealing, AccountancyServices), Set(TransmittingMoney))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
      }
    }

    "redirect to the CustomersOutsideIsUKController" when {
      "post no and has HVD" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "false"
        )

        setupBusinessMatching(Set(HighValueDealing), Set(TransmittingMoney))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
      }
      "not CE, not FX, and not HVD" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "false"
        )

        setupBusinessMatching(Set(AccountancyServices), Set(TransmittingMoney))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
      }
    }

    "redirect to the summary" when {
      "in edit mode" in new Fixture {
        val newRequest = FakeRequest(POST, routes.SendMoneyToOtherCountryController.post().url).withFormUrlEncodedBody(
          "money" -> "false"
        )

        setupBusinessMatching(Set(HighValueDealing), Set(TransmittingMoney))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))

      }
    }
  }
}
