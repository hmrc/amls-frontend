/*
 * Copyright 2023 HM Revenue & Customs
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
import models.Country
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.registrationprogress.{Completed, Section}
import models.renewal._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import services.{ProgressService, RenewalService, SectionsProvider}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.renewal.summary

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]
    lazy val mockProgressService = mock[ProgressService]
    lazy val mockSectionsProvider = mock[SectionsProvider]
    lazy val view = app.injector.instanceOf[summary]
    val controller = new SummaryController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      renewalService = mockRenewalService, cc = mockMcc,
      progressService = mockProgressService,
      sectionsProvider = mockSectionsProvider,
      summary = view
    )

    when {
      mockSectionsProvider.sections(any[CacheMap])
    } thenReturn Seq.empty[Section]

    when {
      mockRenewalService.getSection(any())(any(),any())
    } thenReturn Future.successful(Section("", Completed, false, mock[Call]))

    val renewalModel = Renewal(
      Some(models.renewal.InvolvedInOtherYes("test")),
      Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),
      Some(AMPTurnover.First),
      Some(CustomersOutsideIsUK(true)),
      Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true,true,Some("other")))))),
      Some(TotalThroughput("01")),
      Some(WhichCurrencies(Seq("EUR"),None,Some(MoneySources(None,None,None)))),
      Some(TransactionsInLast12Months("1500")),
      Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("123")),
      Some(FXTransactionsInLast12Months("12")),
      false,
      hasAccepted = true)

  }

    val mockCacheMap = mock[CacheMap]

    val bmBusinessActivities = Some(BMBusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)))

  "Get" must {

    "load the summary page when there is data in the renewal" in new Fixture {

      when(mockDataCacheConnector.fetchAll(any())(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Renewal](Renewal.key))
        .thenReturn(Some(Renewal(Some(models.renewal.InvolvedInOtherYes("test")))))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the renewal progress page when section data is unavailable" in new Fixture {
      when(mockDataCacheConnector.fetchAll(any())(any()))
        .thenReturn(Future.successful(Some(emptyCache)))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "POST" must {
    "update the hasAccepted flag on the model" in new Fixture {
      val cache = mock[CacheMap]

      when {
        controller.dataCacheConnector.fetch[Renewal](any(), any())(any(), any())
      } thenReturn Future.successful(Some(renewalModel.copy(hasAccepted = false)))

      when {
        controller.dataCacheConnector.save[Renewal](any(), eqTo(Renewal.key), any())(any(), any())
      } thenReturn Future.successful(cache)

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.renewal.routes.RenewalProgressController.get.url)

      val captor = ArgumentCaptor.forClass(classOf[Renewal])
      verify(controller.dataCacheConnector).save[Renewal](any(), eqTo(Renewal.key), captor.capture())(any(), any())
      captor.getValue.hasAccepted mustBe true
    }
  }
}
