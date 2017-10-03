/*
 * Copyright 2017 HM Revenue & Customs
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

package services

import connectors._
import models.aboutthebusiness.{AboutTheBusiness, ContactingYou, NonUKCorrespondenceAddress}
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.{CustomersOutsideUK => BACustomersOutsideUK, InvolvedInOtherYes => BAInvolvedInOtherYes, _}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.{Hvd, PaymentMethods, PercentageOfCashPaymentOver15000, ReceiveCashPayments}
import models.moneyservicebusiness.{MostTransactions => MsbMostTransactions, SendTheLargestAmountsOfMoney => MsbSendTheLargestAmountsOfMoney, WhichCurrencies => MsbWhichCurrencies, _}
import models.renewal.{PaymentMethods => RPaymentMethods, PercentageOfCashPaymentOver15000 => RPercentageOfCashPaymentOver15000, ReceiveCashPayments => RReceiveCashPayments, _}
import models.responsiblepeople.ResponsiblePeople
import models.status.{RenewalSubmitted, SubmissionReadyForReview}
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{Country, ViewResponse}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Writes
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}
import play.api.http.Status.OK

class LandingServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with OneAppPerSuite with FutureAwaits with DefaultAwaitTimeout {

  object TestLandingService extends LandingService {
    override private[services] val cacheConnector = mock[DataCacheConnector]
    override private[services] val keyStore = mock[KeystoreConnector]
    override private[services] val desConnector = mock[AmlsConnector]
    override private[services] val statusService = mock[StatusService]
    override private[services] val businessMatchingConnector = mock[BusinessMatchingConnector]
  }

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]
  implicit val ec = mock[ExecutionContext]

  "hasSavedFrom" must {

    "return true if a cache exists" in {
      when {
        TestLandingService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(CacheMap("", Map.empty)))

      whenReady (TestLandingService.hasSavedForm) {
        _ mustEqual true
      }
    }

    "return false if a cache does not exist" in {
      when {
        TestLandingService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(None)

      whenReady (TestLandingService.hasSavedForm) {
        _ mustEqual false
      }
    }
  }

  "setAltCorrespondenceAddress" must {

    val cacheMap = CacheMap("", Map.empty)


    "return a cachmap with the saved alternative correspondence address - true" in {
      val correspondenceAddress = NonUKCorrespondenceAddress("Name Test", "Test", "Test", "Test", Some("test"), None, Country("Albania", "AL"))
      val aboutTheBusiness = AboutTheBusiness(None, None, None, None, None,None, None, Some(correspondenceAddress))

      implicit val r = FakeRequest()

      when(TestLandingService.cacheConnector.save[AboutTheBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(cacheMap))

      def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section: T) = {
        when {
          mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
        } thenReturn Future.successful(result)
      }

      setUpMockView(TestLandingService.cacheConnector, cacheMap, AboutTheBusiness.key, aboutTheBusiness.copy(altCorrespondenceAddress = Some(true)))


      await(TestLandingService.setAltCorrespondenceAddress(aboutTheBusiness)) mustEqual cacheMap

    }

    "return a cachmap with the saved alternative correspondence address - false" in {
      implicit val r = FakeRequest()

      val aboutTheBusiness = AboutTheBusiness(None, None, None, None, None,None, None, None)

      when(TestLandingService.cacheConnector.save[AboutTheBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(cacheMap))

      def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section: T) = {
        when {
          mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
        } thenReturn Future.successful(result)
      }

      setUpMockView(TestLandingService.cacheConnector, cacheMap, AboutTheBusiness.key, aboutTheBusiness.copy(altCorrespondenceAddress = Some(false)))


      await(TestLandingService.setAltCorrespondenceAddress(aboutTheBusiness)) mustEqual cacheMap

    }
  }

  "setAlCorrespondenceAddressWithRegNo" must {

    val cacheMap = CacheMap("", Map.empty)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = None,
      tradingPremisesSection = None,
      aboutTheBusinessSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)) ),
      businessActivitiesSection = None,
      responsiblePeopleSection = None,
      tcspSection = None,
      aspSection = None,
      msbSection = None,
      hvdSection = None,
      supervisionSection = None
    )

    def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section : T) = {
      when {
        mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
      } thenReturn Future.successful(result)
    }

    "return a cachmap of the saved alternative correspondence addres" in {

      when(TestLandingService.cacheConnector.save[AboutTheBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(cacheMap))

      when {
        TestLandingService.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)

      setUpMockView(TestLandingService.cacheConnector, cacheMap, AboutTheBusiness.key, viewResponse.aboutTheBusinessSection.copy(altCorrespondenceAddress = Some(true)))

      await(TestLandingService.setAlCorrespondenceAddressWithRegNo("regNo")) mustEqual cacheMap
    }

  }

  "refreshCache" must {
    val cacheMap = CacheMap("", Map.empty)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = None,
      tradingPremisesSection = None,
      aboutTheBusinessSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)) ),
      businessActivitiesSection = None,
      responsiblePeopleSection = None,
      tcspSection = None,
      aspSection = None,
      msbSection = None,
      hvdSection = None,
      supervisionSection = None
    )

    def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section : T) = {
      when {
        mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
      } thenReturn Future.successful(result)
    }

    "return a cachMap of the saved sections" in {
      when(TestLandingService.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionReadyForReview))

      when {
        TestLandingService.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)

      val user = mock[LoggedInUser]

      when(ac.user).thenReturn(user)
      when(user.oid).thenReturn("")
      when(TestLandingService.cacheConnector.remove(any())(any())).thenReturn(Future.successful(HttpResponse(OK)))

      setUpMockView(TestLandingService.cacheConnector, cacheMap, ViewResponse.key, Some(viewResponse))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BusinessMatching.key, viewResponse.businessMatchingSection.copy(hasAccepted = true))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, EstateAgentBusiness.key, Some(viewResponse.eabSection.copy(hasAccepted = true)))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, TradingPremises.key, viewResponse.tradingPremisesSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, AboutTheBusiness.key, viewResponse.aboutTheBusinessSection.copy(hasAccepted = true))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BankDetails.key, viewResponse.bankDetailsSection.map(b => b.copy(hasAccepted = true)))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, AddPerson.key, viewResponse.aboutYouSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BusinessActivities.key, viewResponse.businessActivitiesSection.copy(hasAccepted = true))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, ResponsiblePeople.key, viewResponse.responsiblePeopleSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Tcsp.key, Some(viewResponse.tcspSection.copy(hasAccepted = true)))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Asp.key, Some(viewResponse.aspSection.copy(hasAccepted = true)))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, MoneyServiceBusiness.key, Some(viewResponse.msbSection.copy(hasAccepted = true)))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Hvd.key, Some(viewResponse.hvdSection.copy(hasAccepted = true)))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Supervision.key, Some(viewResponse.supervisionSection.copy(hasAccepted = true)))

      await(TestLandingService.refreshCache("regNo")) mustEqual cacheMap
    }

  }

  "refreshCache when status is renewalSubmitted" must {

    val businessActivitiesSection = BusinessActivities(expectedAMLSTurnover = Some(ExpectedAMLSTurnover.First),
      involvedInOther = Some(BAInvolvedInOtherYes("test")),
      customersOutsideUK = Some(BACustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      expectedBusinessTurnover = Some(ExpectedBusinessTurnover.First),
      hasAccepted = true
    )

    val msbSection = MoneyServiceBusiness(
      throughput = Some(ExpectedThroughput.Second),
      transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
      sendTheLargestAmountsOfMoney = Some(MsbSendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
      mostTransactions = Some(MsbMostTransactions(Seq(Country("United Kingdom", "GB")))),
      ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
      whichCurrencies = Some(MsbWhichCurrencies(Seq("USD", "GBP", "EUR"),None, None, None, None)),
      hasAccepted = true
    )

    val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))
    val renewalPaymentMethods = RPaymentMethods(courier = true, direct = true, other = Some("foo"))

    val hvdSection  = Hvd(percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
    receiveCashPayments = Some(ReceiveCashPayments(Some(paymentMethods))), hasAccepted = true)

    val renewalModel = Renewal(Some(InvolvedInOtherYes("test")),Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),Some(CustomersOutsideUK(Some(List(Country("United Kingdom","GB"))))),
      Some(RPercentageOfCashPaymentOver15000.First),Some(RReceiveCashPayments(Some(renewalPaymentMethods))),
      Some(TotalThroughput("02")),Some(WhichCurrencies(List("USD", "GBP", "EUR"),None,None,None,None)),
      Some(TransactionsInLast12Months("12345678963")),
      Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"),None,None)),
      Some(MostTransactions(List(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("12345678963")),
      false)

    val cacheMap = CacheMap("", Map.empty)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(hasAccepted = true),
      eabSection = None,
      tradingPremisesSection = None,
      aboutTheBusinessSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)) ),
      businessActivitiesSection = businessActivitiesSection,
      responsiblePeopleSection = None,
      tcspSection = None,
      aspSection = None,
      msbSection = Some(msbSection),
      hvdSection = Some(hvdSection),
      supervisionSection = None
    )

    def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section : T) = {
      when {
        mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
      } thenReturn Future.successful(result)
    }

    "return a cachMap of the saved sections" in {

      when(TestLandingService.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(RenewalSubmitted(None)))

      when {
        TestLandingService.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)

      val user = mock[LoggedInUser]

      when(ac.user).thenReturn(user)
      when(user.oid).thenReturn("")
      when(TestLandingService.cacheConnector.remove(any())(any())).thenReturn(Future.successful(HttpResponse(OK)))

      setUpMockView(TestLandingService.cacheConnector, cacheMap, BusinessMatching.key, viewResponse.businessMatchingSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, EstateAgentBusiness.key, viewResponse.eabSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, TradingPremises.key, viewResponse.tradingPremisesSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, AboutTheBusiness.key, viewResponse.aboutTheBusinessSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BankDetails.key, viewResponse.bankDetailsSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, AddPerson.key, viewResponse.aboutYouSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BusinessActivities.key, viewResponse.businessActivitiesSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, ResponsiblePeople.key, viewResponse.responsiblePeopleSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Tcsp.key, viewResponse.tcspSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Asp.key, viewResponse.aspSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, MoneyServiceBusiness.key, viewResponse.msbSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Hvd.key, viewResponse.hvdSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Supervision.key, viewResponse.supervisionSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Renewal.key, renewalModel)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, ViewResponse.key, Some(viewResponse))

      whenReady(TestLandingService.refreshCache("regNo")){
        _ mustEqual cacheMap
      }
    }

  }

  "reviewDetails" must {
    "contact the business matching service for the business details" in {
      val model = BusinessMatchingReviewDetails(
        "Test Business",
        None,
        BusinessMatchingAddress("Line 1", "Line 2", None, None, None, "United Kingdom"),
        "sap number",
        "safe id",
        agentReferenceNumber = Some("test")
      )

      implicit val r = FakeRequest()

      when {
        TestLandingService.businessMatchingConnector.getReviewDetails(any())
      } thenReturn Future.successful(Some(model))

      await(TestLandingService.reviewDetails)

      verify(TestLandingService.businessMatchingConnector).getReviewDetails(any())
    }
  }

  "updateReviewDetails" must {

    val cacheMap = CacheMap("", Map.empty)
    val reviewDetails = ReviewDetails(
      businessName = "",
      businessType = None,
      businessAddress = Address(
        line_1 = "",
        line_2 = "",
        line_3 = None,
        line_4 = None,
        postcode = None,
        country = Country("United Kingdom", "GB")
      ),
      safeId = ""
    )

    "save BusinessMatching succeed" in {
      when {
        TestLandingService.cacheConnector.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
      } thenReturn Future.successful(cacheMap)

      whenReady (TestLandingService.updateReviewDetails(reviewDetails)) {
        _ mustEqual cacheMap
      }
    }

    "pass back a failed future when updating BusinessMatching fails" in {
      when {
        TestLandingService.cacheConnector.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
      } thenReturn Future.failed(new Exception(""))

      whenReady (TestLandingService.updateReviewDetails(reviewDetails).failed) {
        _ mustBe an[Exception]
      }
    }

  }
}
