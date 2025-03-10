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
import models.Country
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.registrationprogress.{Completed, TaskRow}
import models.renewal._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.Injecting
import services.{ProgressService, RenewalService}
import services.cache.Cache
import utils.AmlsSpec
import utils.renewal.CheckYourAnswersHelper
import views.html.renewal.CheckYourAnswersView

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[Cache]

    val emptyCache = Cache.empty

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService     = mock[RenewalService]
    lazy val mockProgressService    = mock[ProgressService]
    lazy val view                   = inject[CheckYourAnswersView]
    val controller                  = new SummaryController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      progressService = mockProgressService,
      cyaHelper = inject[CheckYourAnswersHelper],
      view = view
    )

    when {
      mockRenewalService.getTaskRow(any())(any())
    } thenReturn Future.successful(TaskRow("", "/foo", false, Completed, TaskRow.completedTag))

    val renewalModel = Renewal(
      Some(models.renewal.InvolvedInOtherYes("test")),
      Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),
      Some(AMPTurnover.First),
      Some(CustomersOutsideIsUK(true)),
      Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(
        CashPayments(
          CashPaymentsCustomerNotMet(true),
          Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other"))))
        )
      ),
      Some(TotalThroughput("01")),
      Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
      Some(TransactionsInLast12Months("1500")),
      Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("123")),
      Some(FXTransactionsInLast12Months("12")),
      false,
      hasAccepted = true
    )

  }

  val mockCacheMap = mock[Cache]

  val bmBusinessActivities = Some(
    BMBusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
  )

  "Get" must {

    "load the summary page when there is data in the renewal" in new Fixture {

      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Renewal](Renewal.key))
        .thenReturn(Some(Renewal(Some(models.renewal.InvolvedInOtherYes("test")))))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the renewal progress page when section data is unavailable" in new Fixture {
      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(emptyCache)))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "POST" must {
    "update the hasAccepted flag on the model" in new Fixture {
      val cache = mock[Cache]

      when {
        controller.dataCacheConnector.fetch[Renewal](any(), any())(any())
      } thenReturn Future.successful(Some(renewalModel.copy(hasAccepted = false)))

      when {
        controller.dataCacheConnector.save[Renewal](any(), eqTo(Renewal.key), eqTo(renewalModel))(any())
      } thenReturn Future.successful(cache)

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.renewal.routes.RenewalProgressController.get.url)

      val captor = ArgumentCaptor.forClass(classOf[Renewal])
      verify(controller.dataCacheConnector).save[Renewal](any(), eqTo(Renewal.key), captor.capture())(any())
      captor.getValue mustBe renewalModel
      captor.getValue.hasAccepted mustBe true
    }
  }
}
