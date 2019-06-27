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

package services

import connectors._
import models.asp.{Accountancy, BookKeeping, ServicesOfBusiness}
import models.businessdetails.{BusinessDetails, CorrespondenceAddress, CorrespondenceAddressIsUk, CorrespondenceAddressNonUk}
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.{CustomersOutsideUK => BACustomersOutsideUK, InvolvedInOtherYes => BAInvolvedInOtherYes, _}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.estateagentbusiness.{Auction, EstateAgentBusiness, Residential, Services}
import models.hvd._
import models.moneyservicebusiness.{MostTransactions => MsbMostTransactions, SendTheLargestAmountsOfMoney => MsbSendTheLargestAmountsOfMoney, WhichCurrencies => MsbWhichCurrencies, _}
import models.renewal.{MoneySources => RMoneySources, PaymentMethods => RPaymentMethods, PercentageOfCashPaymentOver15000 => RPercentageOfCashPaymentOver15000, ReceiveCashPayments => RReceiveCashPayments, WhichCurrencies => RenWhichCurrencies, _}
import models.responsiblepeople.ResponsiblePerson
import models.status.{RenewalSubmitted, SubmissionReadyForReview}
import models.supervision.Supervision
import models.tcsp._
import models.tradingpremises.TradingPremises
import models._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Writes
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser}
import utils.AmlsSpec

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class LandingServiceSpec extends AmlsSpec with ScalaFutures with FutureAwaits with DefaultAwaitTimeout {

  val service = new LandingService (
    cacheConnector = mock[DataCacheConnector],
    keyStore = mock[KeystoreConnector],
    desConnector = mock[AmlsConnector],
    statusService = mock[StatusService],
    businessMatchingConnector = mock[BusinessMatchingConnector]
  )

  val msbSection = MoneyServiceBusiness(
    throughput = Some(ExpectedThroughput.Second),
    transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
    sendTheLargestAmountsOfMoney = Some(MsbSendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
    mostTransactions = Some(MsbMostTransactions(Seq(Country("United Kingdom", "GB")))),
    ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
    whichCurrencies = Some(MsbWhichCurrencies(Seq("USD", "GBP", "EUR"),None, None)),
    hasAccepted = true
  )

  val eabSection = Some(EstateAgentBusiness(Some(Services(Set(Auction, Residential))), None, None, None))
  val tcspTypes = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, CompanyDirectorEtc))
  val tcspSection = Some(Tcsp(Some(tcspTypes)))
  val aspSection = Some(Asp(Some(ServicesOfBusiness(Set(BookKeeping, Accountancy))), None))

  val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))
  val renewalPaymentMethods = RPaymentMethods(courier = true, direct = true, other = Some("foo"))

  val hvdSection  = Hvd(
    products = Some(Products(Set(Alcohol, Tobacco))),
    percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
    receiveCashPayments = Some(true),
    cashPaymentMethods = Some(paymentMethods),
    hasAccepted = true
  )

  "setAltCorrespondenceAddress" must {

    val cacheMap = CacheMap("", Map.empty)

    "return a cachmap with the saved alternative correspondence address - true" in {
      val correspondenceAddress = CorrespondenceAddressNonUk("Name Test", "Test", "Test", "Test", Some("test"), None, Country("Albania", "AL"))
      val businessDetails = BusinessDetails(None, None, None, None, None,None, None, Some(CorrespondenceAddressIsUk(Some(false))), Some(CorrespondenceAddress(None, Some(correspondenceAddress))))

      implicit val r = FakeRequest()

      when(service.cacheConnector.save[BusinessDetails](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(cacheMap))

      def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section: T) = {
        when {
          mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
        } thenReturn Future.successful(result)
      }

      setUpMockView(service.cacheConnector, cacheMap, BusinessDetails.key, businessDetails.copy(altCorrespondenceAddress = Some(true)))


      await(service.setAltCorrespondenceAddress(businessDetails)) mustEqual cacheMap

    }

    "return a cachmap with the saved alternative correspondence address - false" in {
      implicit val r = FakeRequest()

      val businessDetails = BusinessDetails(None, None, None, None, None,None, None, None)

      when(service.cacheConnector.save[BusinessDetails](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(cacheMap))

      def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section: T) = {
        when {
          mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
        } thenReturn Future.successful(result)
      }

      setUpMockView(service.cacheConnector, cacheMap, BusinessDetails.key, businessDetails.copy(altCorrespondenceAddress = Some(false)))


      await(service.setAltCorrespondenceAddress(businessDetails)) mustEqual cacheMap

    }
  }

  "setAlCorrespondenceAddressWithRegNo" must {

    val cacheMap = CacheMap("", Map.empty)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = eabSection,
      tradingPremisesSection = None,
      businessDetailsSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)) ),
      businessActivitiesSection = None,
      responsiblePeopleSection = None,
      tcspSection = tcspSection,
      aspSection = aspSection,
      msbSection = Some(msbSection),
      hvdSection = Some(hvdSection),
      supervisionSection = None
    )

    def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section : T) = {
      when {
        mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
      } thenReturn Future.successful(result)
    }

    "return a cachmap of the saved alternative correspondence address" in {

      val cache = mock[CacheMap]

      when(service.cacheConnector.save[BusinessDetails](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(cache))

      when {
        cache.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any())
      } thenReturn None

      when {
        service.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)

      setUpMockView(service.cacheConnector, cache, BusinessDetails.key, viewResponse.businessDetailsSection.copy(altCorrespondenceAddress = Some(true)))

      await(service.setAltCorrespondenceAddress("regNo", None)) mustEqual cache

      verify(service.desConnector).view(any())(any(), any(), any(), any())
    }

    "only call API 5 data" when {
      "the cache doesn't contain a ViewResponse entry" in {
        val cache = mock[CacheMap]

        reset(service.cacheConnector)
        reset(service.desConnector)

        val model = BusinessDetails()

        when {
          cache.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any())
        } thenReturn Some(model)

        when(service.cacheConnector.save[BusinessDetails](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(cacheMap))

        setUpMockView(service.cacheConnector, cache, BusinessDetails.key, viewResponse.businessDetailsSection.copy(altCorrespondenceAddress = Some(true)))

        await(service.setAltCorrespondenceAddress("regNo", Some(cache)))

        verify(service.desConnector, never()).view(any())(any(), any(), any(), any())
      }
    }

  }

  "refreshCache" must {
    val cacheMap = CacheMap("", Map.empty)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = eabSection,
      tradingPremisesSection = None,
      businessDetailsSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)) ),
      businessActivitiesSection = None,
      responsiblePeopleSection = None,
      tcspSection = tcspSection,
      aspSection = aspSection,
      msbSection = Some(msbSection),
      hvdSection = Some(hvdSection),
      supervisionSection = None
    )

    "update all saved sections" in {
      reset(service.cacheConnector)

      when(service.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionReadyForReview))

      when {
        service.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)

      val user = mock[LoggedInUser]

      when(authContext.user).thenReturn(user)
      when(user.oid).thenReturn("")
      when(service.cacheConnector.remove(any(), any())).thenReturn(Future.successful(true))

      when {
        service.cacheConnector.fetchAllWithDefault
      } thenReturn Future.successful(cacheMap)

      val subscriptionResponse = mock[SubscriptionResponse]
      val amendVariationResponse = mock[AmendVariationRenewalResponse]

      when {
        service.cacheConnector.fetch[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any(), any(), any())
      } thenReturn Future.successful(Some(subscriptionResponse))

      when {
        service.cacheConnector.fetch[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any(), any(), any())
      } thenReturn Future.successful(Some(amendVariationResponse))

      when {
        service.cacheConnector.saveAll(any())(any(),any())
      } thenReturn Future.successful(cacheMap)

      await(service.refreshCache("regNo")) mustEqual cacheMap

      verify(service.cacheConnector).upsert(any(), eqTo(ViewResponse.key),
        eqTo(Some(viewResponse)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessMatching.key),
        eqTo(viewResponse.businessMatchingSection.copy(hasAccepted = true, preAppComplete = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(EstateAgentBusiness.key),
        eqTo(Some(viewResponse.eabSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(TradingPremises.key),
        eqTo(Some(viewResponse.tradingPremisesSection.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessDetails.key),
        eqTo(viewResponse.businessDetailsSection.copy(hasAccepted = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BankDetails.key),
        eqTo(viewResponse.bankDetailsSection.map(b => b.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(AddPerson.key),
        eqTo(viewResponse.aboutYouSection))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessActivities.key),
        eqTo(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Tcsp.key),
        eqTo(Some(viewResponse.tcspSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Asp.key),
        eqTo(Some(viewResponse.aspSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(MoneyServiceBusiness.key),
        eqTo(Some(viewResponse.msbSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Hvd.key),
        eqTo(Some(viewResponse.hvdSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Supervision.key),
        eqTo(Some(viewResponse.supervisionSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(SubscriptionResponse.key),
        eqTo(Some(subscriptionResponse)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(AmendVariationRenewalResponse.key),
        eqTo(Some(amendVariationResponse)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(ResponsiblePerson.key),
        eqTo(Some(viewResponse.responsiblePeopleSection.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))))(any(), any(), any())
    }
  }

  "refreshCache with null sectors" must {
    val cacheMap = CacheMap("", Map.empty)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = None,
      tradingPremisesSection = None,
      businessDetailsSection = None,
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

    "update all saved sections correctly" in {

      reset(service.cacheConnector)

      when(service.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionReadyForReview))

      when {
        service.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)

      val user = mock[LoggedInUser]

      when(authContext.user).thenReturn(user)
      when(user.oid).thenReturn("")
      when(service.cacheConnector.remove(any(), any())).thenReturn(Future.successful(true))

      when {
        service.cacheConnector.fetchAllWithDefault
      } thenReturn Future.successful(cacheMap)

      val subscriptionResponse = mock[SubscriptionResponse]
      val amendVariationResponse = mock[AmendVariationRenewalResponse]

      when {
        service.cacheConnector.fetch[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any(), any(), any())
      } thenReturn Future.successful(Some(subscriptionResponse))

      when {
        service.cacheConnector.fetch[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any(), any(), any())
      } thenReturn Future.successful(Some(amendVariationResponse))

      when {
        service.cacheConnector.saveAll(any())(any(),any())
      } thenReturn Future.successful(cacheMap)

      await(service.refreshCache("regNo")) mustEqual cacheMap

      verify(service.cacheConnector).upsert(any(), eqTo(ViewResponse.key),
        eqTo(Some(viewResponse)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessMatching.key),
        eqTo(viewResponse.businessMatchingSection.copy(hasAccepted = true, preAppComplete = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(EstateAgentBusiness.key),
        eqTo(None))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(TradingPremises.key),
        eqTo(Some(viewResponse.tradingPremisesSection.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessDetails.key),
        eqTo(viewResponse.businessDetailsSection.copy(hasAccepted = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BankDetails.key),
        eqTo(viewResponse.bankDetailsSection.map(b => b.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(AddPerson.key),
        eqTo(viewResponse.aboutYouSection))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessActivities.key),
        eqTo(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Tcsp.key),
        eqTo(None))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Asp.key),
        eqTo(None))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(MoneyServiceBusiness.key),
        eqTo(None))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Hvd.key),
        eqTo(None))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Supervision.key),
        eqTo(Some(viewResponse.supervisionSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(SubscriptionResponse.key),
        eqTo(Some(subscriptionResponse)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(AmendVariationRenewalResponse.key),
        eqTo(Some(amendVariationResponse)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(ResponsiblePerson.key),
        eqTo(Some(viewResponse.responsiblePeopleSection.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))))(any(), any(), any())
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
      sendTheLargestAmountsOfMoney = Some(MsbSendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      mostTransactions = Some(MsbMostTransactions(Seq(Country("United Kingdom", "GB")))),
      ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
      whichCurrencies = Some(MsbWhichCurrencies(Seq("USD", "GBP", "EUR"), None, None)),
      hasAccepted = true
    )

    val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))
    val renewalPaymentMethods = RPaymentMethods(courier = true, direct = true, other = Some("foo"))

    val hvdSection  = Hvd(
      products = Some(Products(Set(Alcohol, Tobacco))),
      percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
      receiveCashPayments = Some(true),
      cashPaymentMethods = Some(paymentMethods),
      hasAccepted = true
    )

    val renewalModel = Renewal(Some(InvolvedInOtherYes("test")),Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),Some(CustomersOutsideUK(Some(List(Country("United Kingdom","GB"))))),
      Some(RPercentageOfCashPaymentOver15000.First),Some(RReceiveCashPayments(Some(renewalPaymentMethods))),
      Some(TotalThroughput("02")),Some(RenWhichCurrencies(Seq("USD"), None, Some(RMoneySources(None, None, None)))),
      Some(TransactionsInLast12Months("12345678963")),
      Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      Some(MostTransactions(List(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("12345678963")),
      Some(FXTransactionsInLast12Months("3987654321")),
      false)

    val cacheMap = CacheMap("", Map.empty)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(hasAccepted = true),
      eabSection = eabSection,
      tradingPremisesSection = None,
      businessDetailsSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder)) ),
      businessActivitiesSection = businessActivitiesSection,
      responsiblePeopleSection = None,
      tcspSection = tcspSection,
      aspSection = aspSection,
      msbSection = Some(msbSection),
      hvdSection = Some(hvdSection),
      supervisionSection = None
    )

    "update all saved sections" in {
      reset(service.cacheConnector)

      when(service.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(RenewalSubmitted(None)))

      when {
        service.cacheConnector.fetchAllWithDefault
      } thenReturn Future.successful(cacheMap)

      when {
        service.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)

      when {
        service.cacheConnector.fetch[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any(), any(), any())
      } thenReturn Future.successful(None)

      when {
        service.cacheConnector.fetch[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any(), any(), any())
      } thenReturn Future.successful(None)

      val user = mock[LoggedInUser]

      when(authContext.user).thenReturn(user)
      when(user.oid).thenReturn("")
      when(service.cacheConnector.remove(any(), any())).thenReturn(Future.successful(true))

      when {
        service.cacheConnector.saveAll(any())(any(), any())
      } thenReturn Future.successful(cacheMap)

      await(service.refreshCache("regNo")) mustEqual cacheMap

      verify(service.cacheConnector).upsert(any(), eqTo(ViewResponse.key),
        eqTo(Some(viewResponse)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessMatching.key),
        eqTo(viewResponse.businessMatchingSection.copy(hasAccepted = true, preAppComplete = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(EstateAgentBusiness.key),
        eqTo(Some(viewResponse.eabSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(TradingPremises.key),
        eqTo(Some(viewResponse.tradingPremisesSection.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessDetails.key),
        eqTo(viewResponse.businessDetailsSection.copy(hasAccepted = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BankDetails.key),
        eqTo(viewResponse.bankDetailsSection.map(b => b.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(AddPerson.key),
        eqTo(viewResponse.aboutYouSection))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(BusinessActivities.key),
        eqTo(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Tcsp.key),
        eqTo(Some(viewResponse.tcspSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(Asp.key),
        eqTo(Some(viewResponse.aspSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(MoneyServiceBusiness.key),
        eqTo(Some(viewResponse.msbSection.copy(hasAccepted = true))))(any(), any(), any())

      verify(service.cacheConnector).upsert(any(), eqTo(Hvd.key),
        eqTo(Some(viewResponse.hvdSection.copy(hasAccepted = true))))(any(), any(), any())

      verify(service.cacheConnector).upsert(any(), eqTo(Supervision.key),
        eqTo(Some(viewResponse.supervisionSection.copy(hasAccepted = true))))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(SubscriptionResponse.key),
        eqTo(None))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(AmendVariationRenewalResponse.key),
        eqTo(None))(any(), any(), any())
      verify(service.cacheConnector).upsert(any(), eqTo(ResponsiblePerson.key),
        eqTo(Some(viewResponse.responsiblePeopleSection.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))))(any(), any(), any())
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
        service.businessMatchingConnector.getReviewDetails(any())
      } thenReturn Future.successful(Some(model))

      await(service.reviewDetails)

      verify(service.businessMatchingConnector).getReviewDetails(any())
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
        service.cacheConnector.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
      } thenReturn Future.successful(cacheMap)

      whenReady (service.updateReviewDetails(reviewDetails)) {
        _ mustEqual cacheMap
      }
    }

    "pass back a failed future when updating BusinessMatching fails" in {
      when {
        service.cacheConnector.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
      } thenReturn Future.failed(new Exception(""))

      whenReady (service.updateReviewDetails(reviewDetails).failed) {
        _ mustBe an[Exception]
      }
    }

  }
}
