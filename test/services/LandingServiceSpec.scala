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

package services

import connectors._
import models._
import models.amp.Amp
import models.asp.Service._
import models.asp.{Asp, ServicesOfBusiness}
import models.bankdetails.BankDetails
import models.businessactivities.{CustomersOutsideUK => BACustomersOutsideUK, InvolvedInOtherYes => BAInvolvedInOtherYes, _}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.{BusinessDetails, CorrespondenceAddress, CorrespondenceAddressIsUk, CorrespondenceAddressNonUk}
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.eab.Eab
import models.hvd.Products.{Alcohol, Tobacco}
import models.hvd._
import models.moneyservicebusiness.{MostTransactions => MsbMostTransactions, SendTheLargestAmountsOfMoney => MsbSendTheLargestAmountsOfMoney, WhichCurrencies => MsbWhichCurrencies, _}
import models.renewal.{PaymentMethods => RPaymentMethods}
import models.responsiblepeople.ResponsiblePerson
import models.status.{RenewalSubmitted, SubmissionReadyForReview}
import models.supervision.Supervision
import models.tcsp.TcspTypes._
import models.tcsp._
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{Json, Writes}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import services.cache.Cache
import uk.gov.hmrc.http.HeaderCarrier
import utils.AmlsSpec

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class LandingServiceSpec extends AmlsSpec with ScalaFutures with FutureAwaits with DefaultAwaitTimeout {

  val service = new LandingService (
    cacheConnector = mock[DataCacheConnector],
    amlsConnector = mock[AmlsConnector],
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

  val completeServiceList = Seq("auctioneering", "residential")

  val completeServices = Json.obj("eabServicesProvided" -> completeServiceList )

  val completeDateOfChange = Json.obj(
    "dateOfChange" -> "2019-01-01"
  )

  val completeEstateAgencyActPenalty = Json.obj(
    "penalisedEstateAgentsAct" -> true,
    "penalisedEstateAgentsActDetail" -> "details"
  )

  val completePenalisedProfessionalBody = Json.obj(
    "penalisedProfessionalBody" -> true,
    "penalisedProfessionalBodyDetail" -> "details"
  )

  val completeRedressScheme = Json.obj(
    "redressScheme" -> "propertyRedressScheme",
    "redressSchemeDetail" -> "null"
  )

  val completeMoneyProtectionScheme = Json.obj(
    "clientMoneyProtectionScheme" -> true
  )

  val completeData = completeServices ++
    completeDateOfChange ++
    completeEstateAgencyActPenalty ++
    completePenalisedProfessionalBody ++
    completeRedressScheme ++
    completeMoneyProtectionScheme

  val eabSection = Eab(completeData,  hasAccepted = true)

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

  val credId = "internalId"

  val ampSection = Amp(
    data = Json.obj(
      "typeOfParticipant"     -> Seq("artGalleryOwner"),
      "soldOverThreshold"     -> true,
      "dateTransactionOverThreshold"  -> LocalDate.now,
      "identifyLinkedTransactions"    -> true,
      "percentageExpectedTurnover"    -> "fortyOneToSixty"
    ),
    hasAccepted = false,
    hasChanged = false
  )

  def setUpMockView[T](mock: DataCacheConnector, result: Cache, key: String, section: T) = {
    when {
      mock.save[T](any(), eqTo(key), eqTo(section))(any())
    } thenReturn Future.successful(result)
  }

  "setAltCorrespondenceAddress" must {

    val cacheMap = Cache.empty

    "return a cachmap with the saved alternative correspondence address - true" in {
      val correspondenceAddress = CorrespondenceAddressNonUk("Name Test", "Test", "Test", Some("Test"), Some("test"), None, Country("Albania", "AL"))
      val businessDetails = BusinessDetails(None, None, None, None, None, None, None, None, Some(CorrespondenceAddressIsUk(false)), Some(CorrespondenceAddress(None, Some(correspondenceAddress))))

      when(service.cacheConnector.save[BusinessDetails](any(), any(), any())(any()))
        .thenReturn(Future.successful(cacheMap))

      setUpMockView(service.cacheConnector, cacheMap, BusinessDetails.key, businessDetails.copy(altCorrespondenceAddress = Some(true)))


      await(service.setAltCorrespondenceAddress(businessDetails, credId)) mustEqual cacheMap

    }

    "return a cachmap with the saved alternative correspondence address - false" in {

      val businessDetails = BusinessDetails(None, None, None, None, None,None, None, None)

      when(service.cacheConnector.save[BusinessDetails](any(), any(), any())(any()))
        .thenReturn(Future.successful(cacheMap))

      setUpMockView(service.cacheConnector, cacheMap, BusinessDetails.key, businessDetails.copy(altCorrespondenceAddress = Some(false)))

      await(service.setAltCorrespondenceAddress(businessDetails, credId)) mustEqual cacheMap

    }
  }

  "setAlCorrespondenceAddressWithRegNo" must {

    val cacheMap = Cache.empty

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = Some(eabSection),
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
      ampSection = Some(ampSection),
      supervisionSection = None
    )

    "return a cachmap of the saved alternative correspondence address" in {

      val cache = mock[Cache]

      when(service.cacheConnector.save[BusinessDetails](any(), any(), any())(any()))
        .thenReturn(Future.successful(cache))

      when {
        cache.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any())
      } thenReturn None

      when {
        service.amlsConnector.view(any[String], any())(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]])
      } thenReturn Future.successful(viewResponse)

      setUpMockView(service.cacheConnector, cache, BusinessDetails.key, viewResponse.businessDetailsSection.copy(altCorrespondenceAddress = Some(true)))

      await(service.setAltCorrespondenceAddress("regNo", None, ("accType", "id"), credId)) mustEqual cache

      verify(service.amlsConnector).view(any(), any())(any(), any(), any())
    }

    "only call API 5 data" when {
      "the cache doesn't contain a ViewResponse entry" in {
        val cache = mock[Cache]

        reset(service.cacheConnector)
        reset(service.amlsConnector)

        val model = BusinessDetails()

        when {
          cache.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any())
        } thenReturn Some(model)

        when(service.cacheConnector.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(cacheMap))

        setUpMockView(service.cacheConnector, cache, BusinessDetails.key, viewResponse.businessDetailsSection.copy(altCorrespondenceAddress = Some(true)))

        await(service.setAltCorrespondenceAddress("regNo", Some(cache), ("accType", "id"), credId))

        verify(service.amlsConnector, never()).view(any(), any())(any(), any(), any())
      }
    }

  }

  "refreshCache" must {
    val cacheMap = Cache.empty

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = Some(eabSection),
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
      ampSection = Some(ampSection),
      supervisionSection = None
    )

    "update all saved sections" in {
      reset(service.cacheConnector)

      when(service.statusService.getStatus(any[Option[String]], any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      when {
        service.amlsConnector.view(any[String], any())(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]])
      } thenReturn Future.successful(viewResponse)

      when(service.cacheConnector.remove(any[String])).thenReturn(Future.successful(true))

      when {
        service.cacheConnector.fetchAllWithDefault(any())
      } thenReturn Future.successful(cacheMap)

      val subscriptionResponse = mock[SubscriptionResponse]
      val amendVariationResponse = mock[AmendVariationRenewalResponse]

      when {
        service.cacheConnector.fetch[SubscriptionResponse](any(), eqTo(SubscriptionResponse.key))(any())
      } thenReturn Future.successful(Some(subscriptionResponse))

      when {
        service.cacheConnector.fetch[AmendVariationRenewalResponse](any(), eqTo(AmendVariationRenewalResponse.key))(any())
      } thenReturn Future.successful(Some(amendVariationResponse))

      when {
        service.cacheConnector.saveAll(any(), any())
      } thenReturn Future.successful(cacheMap)

      await(service.refreshCache("regNo", credId, ("accType", "id"))) mustEqual cacheMap

      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(ViewResponse.key),
        eqTo(Some(viewResponse)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessMatching.key),
        eqTo(viewResponse.businessMatchingSection.copy(hasAccepted = true, preAppComplete = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Eab.key),
        eqTo(viewResponse.eabSection.map(eab => eab.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(TradingPremises.key),
        eqTo(Some(viewResponse.tradingPremisesSection.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessDetails.key),
        eqTo(viewResponse.businessDetailsSection.copy(hasAccepted = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BankDetails.key),
        eqTo(viewResponse.bankDetailsSection.map(b => b.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(AddPerson.key),
        eqTo(viewResponse.aboutYouSection))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessActivities.key),
        eqTo(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Tcsp.key),
        eqTo(Some(viewResponse.tcspSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Asp.key),
        eqTo(Some(viewResponse.aspSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(MoneyServiceBusiness.key),
        eqTo(Some(viewResponse.msbSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Hvd.key),
        eqTo(Some(viewResponse.hvdSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Supervision.key),
        eqTo(Some(viewResponse.supervisionSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(SubscriptionResponse.key),
        eqTo(Some(subscriptionResponse)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(AmendVariationRenewalResponse.key),
        eqTo(Some(amendVariationResponse)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(ResponsiblePerson.key),
        eqTo(Some(viewResponse.responsiblePeopleSection.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))))(any())
    }
  }

  "refreshCache with null sectors" must {
    val cacheMap = Cache.empty

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
      ampSection = None,
      supervisionSection = None
    )

    "update all saved sections correctly" in {

      reset(service.cacheConnector)

      when(service.statusService.getStatus(any[Option[String]], any(), any())(any(), any(), any())).thenReturn(Future.successful(SubmissionReadyForReview))

      when {
        service.amlsConnector.view(any[String], any())(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]])
      } thenReturn Future.successful(viewResponse)

      when(service.cacheConnector.remove(any())).thenReturn(Future.successful(true))

      when {
        service.cacheConnector.fetchAllWithDefault(any())
      } thenReturn Future.successful(cacheMap)

      val subscriptionResponse = mock[SubscriptionResponse]
      val amendVariationResponse = mock[AmendVariationRenewalResponse]

      when {
        service.cacheConnector.fetch[SubscriptionResponse](any(), eqTo(SubscriptionResponse.key))(any())
      } thenReturn Future.successful(Some(subscriptionResponse))

      when {
        service.cacheConnector.fetch[AmendVariationRenewalResponse](any(), eqTo(AmendVariationRenewalResponse.key))(any())
      } thenReturn Future.successful(Some(amendVariationResponse))

      when {
        service.cacheConnector.saveAll(any(), any())
      } thenReturn Future.successful(cacheMap)

      await(service.refreshCache("regNo", credId, ("accType", "id"))) mustEqual cacheMap

      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(ViewResponse.key),
        eqTo(Some(viewResponse)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessMatching.key),
        eqTo(viewResponse.businessMatchingSection.copy(hasAccepted = true, preAppComplete = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Eab.key),
        eqTo(None))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(TradingPremises.key),
        eqTo(Some(viewResponse.tradingPremisesSection.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessDetails.key),
        eqTo(viewResponse.businessDetailsSection.copy(hasAccepted = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BankDetails.key),
        eqTo(viewResponse.bankDetailsSection.map(b => b.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(AddPerson.key),
        eqTo(viewResponse.aboutYouSection))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessActivities.key),
        eqTo(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Tcsp.key),
        eqTo(None))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Asp.key),
        eqTo(None))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(MoneyServiceBusiness.key),
        eqTo(None))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Hvd.key),
        eqTo(None))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Supervision.key),
        eqTo(Some(viewResponse.supervisionSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(SubscriptionResponse.key),
        eqTo(Some(subscriptionResponse)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(AmendVariationRenewalResponse.key),
        eqTo(Some(amendVariationResponse)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(ResponsiblePerson.key),
        eqTo(Some(viewResponse.responsiblePeopleSection.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))))(any())
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

    val hvdSection  = Hvd(
      products = Some(Products(Set(Alcohol, Tobacco))),
      percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
      receiveCashPayments = Some(true),
      cashPaymentMethods = Some(paymentMethods),
      hasAccepted = true
    )

    val cacheMap = Cache.empty

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(hasAccepted = true),
      eabSection = Some(eabSection),
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
      ampSection = Some(ampSection),
      supervisionSection = None
    )

    "update all saved sections" in {
      reset(service.cacheConnector)

      when(service.statusService.getStatus(any[Option[String]], any(), any())(any(), any(), any())).thenReturn(Future.successful(RenewalSubmitted(None)))

      when {
        service.cacheConnector.fetchAllWithDefault(any())
      } thenReturn Future.successful(cacheMap)

      when {
        service.amlsConnector.view(any[String], any())(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]])
      } thenReturn Future.successful(viewResponse)

      when {
        service.cacheConnector.fetch[SubscriptionResponse](any(), eqTo(SubscriptionResponse.key))(any())
      } thenReturn Future.successful(None)

      when {
        service.cacheConnector.fetch[AmendVariationRenewalResponse](any(), eqTo(AmendVariationRenewalResponse.key))(any())
      } thenReturn Future.successful(None)

      when(service.cacheConnector.remove(any())).thenReturn(Future.successful(true))

      when {
        service.cacheConnector.saveAll(any(), any())
      } thenReturn Future.successful(cacheMap)

      await(service.refreshCache("regNo", credId, ("accType", "id"))) mustEqual cacheMap

      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(ViewResponse.key),
        eqTo(Some(viewResponse)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessMatching.key),
        eqTo(viewResponse.businessMatchingSection.copy(hasAccepted = true, preAppComplete = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Eab.key),
        eqTo(viewResponse.eabSection.map(eab => eab.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(TradingPremises.key),
        eqTo(Some(viewResponse.tradingPremisesSection.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessDetails.key),
        eqTo(viewResponse.businessDetailsSection.copy(hasAccepted = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BankDetails.key),
        eqTo(viewResponse.bankDetailsSection.map(b => b.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(AddPerson.key),
        eqTo(viewResponse.aboutYouSection))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(BusinessActivities.key),
        eqTo(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Tcsp.key),
        eqTo(Some(viewResponse.tcspSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Asp.key),
        eqTo(Some(viewResponse.aspSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(MoneyServiceBusiness.key),
        eqTo(Some(viewResponse.msbSection.copy(hasAccepted = true))))(any())

      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Hvd.key),
        eqTo(Some(viewResponse.hvdSection.copy(hasAccepted = true))))(any())

      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(Supervision.key),
        eqTo(Some(viewResponse.supervisionSection.copy(hasAccepted = true))))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(SubscriptionResponse.key),
        eqTo(None))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(AmendVariationRenewalResponse.key),
        eqTo(None))(any())
      verify(service.cacheConnector).upsertNewAuth(any(), eqTo(ResponsiblePerson.key),
        eqTo(Some(viewResponse.responsiblePeopleSection.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))))(any())
    }
  }

  "reviewDetails" must {
    "contact the business matching service for the business details" in {
      val model = BusinessMatchingReviewDetails(
        "Test Business",
        None,
        BusinessMatchingAddress("Line 1", None, None, None, None, "United Kingdom"),
        "sap number",
        "safe id",
        agentReferenceNumber = Some("test")
      )

      when {
        service.businessMatchingConnector.getReviewDetails(any(), any())
      } thenReturn Future.successful(Some(model))

      await(service.reviewDetails)

      verify(service.businessMatchingConnector).getReviewDetails(any(), any())
    }
  }

  "updateReviewDetails" must {

    val cacheMap = Cache.empty
    val reviewDetails = ReviewDetails(
      businessName = "",
      businessType = None,
      businessAddress = Address(
        line_1 = "",
        line_2 = None,
        line_3 = None,
        line_4 = None,
        postcode = None,
        country = Country("United Kingdom", "GB")
      ),
      safeId = ""
    )

    "save BusinessMatching succeed" in {
      when {
        service.cacheConnector.save[BusinessMatching](any(), eqTo(BusinessMatching.key), any())(any())
      } thenReturn Future.successful(cacheMap)

      whenReady (service.updateReviewDetails(reviewDetails, credId)) {
        _ mustEqual cacheMap
      }
    }

    "pass back a failed future when updating BusinessMatching fails" in {
      when {
        service.cacheConnector.save[BusinessMatching](any(), eqTo(BusinessMatching.key), any())(any())
      } thenReturn Future.failed(new Exception(""))

      whenReady (service.updateReviewDetails(reviewDetails, credId).failed) {
        _ mustBe an[Exception]
      }
    }

  }
}
