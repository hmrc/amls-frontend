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

package services


import config.WSHttp
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.aboutthebusiness._
import models.asp.{Accountancy, Asp, OtherBusinessTaxMattersNo, ServicesOfBusiness}
import models.bankdetails.{BankDetails, PersonalAccount, UKAccount}
import models.businessactivities._
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7.{BeneficialShareholder, RoleWithinBusinessRelease7}
import models.estateagentbusiness.{ProfessionalBodyNo => EABProfessionalBodyNo, _}
import models.hvd._
import models.moneyservicebusiness._
import models.responsiblepeople.ResponsiblePerson
import models.supervision.{ProfessionalBodyYes => SupervisionProfessionalBodyYes, _}
import models.tcsp.{Other, _}
import models.tradingpremises.TradingPremises
import models.{DataImport, _}
import org.joda.time.LocalDate
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global

class UpdateSave4LaterServiceSpec extends AmlsSpec with MockitoSugar
  with ScalaFutures
  with BusinessMatchingGenerator
  with TradingPremisesGenerator
  with ResponsiblePersonGenerator {

  trait Fixture extends DependencyMocks {

    val http = mock[WSHttp]
    val updateSave4LaterService = new UpdateSave4LaterService(http, mockCacheConnector)

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = None,
      tradingPremisesSection = None,
      aboutTheBusinessSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))),
      businessActivitiesSection = None,
      responsiblePeopleSection = None,
      tcspSection = None,
      aspSection = None,
      msbSection = None,
      hvdSection = None,
      supervisionSection = None
    )

    val businessMatching = businessMatchingGen.sample.get
    val estateAgentBusiness = new EstateAgentBusiness(
      Some(Services(Set(Commercial))),
      Some(ThePropertyOmbudsman),
      Some(EABProfessionalBodyNo),
      Some(PenalisedUnderEstateAgentsActNo),
      false,
      false)

    val tradingPremises = Seq(tradingPremisesGen.sample.get, tradingPremisesGen.sample.get)

    val aboutTheBusiness = AboutTheBusiness(
      previouslyRegistered = Some(PreviouslyRegisteredYes("12345678")),
      activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))),
      vatRegistered = Some(VATRegisteredYes("123456789")),
      corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1234567890")),
      contactingYou = Some(ContactingYou(Some("1234567890"), Some("test@test.com"))),
      registeredOffice = Some(RegisteredOfficeUK("38B", "line2", None, None, "AA1 1AA")),
      altCorrespondenceAddress = Some(true),
      correspondenceAddress = Some(UKCorrespondenceAddress("Name",
        "Business Name",
        "address 1",
        "address 2",
        Some("address 3"),
        Some("address 4"),
        "AA11 1AA")),
      hasAccepted = true
    )

    val bankDetails = BankDetails(Some(PersonalAccount), None, Some(UKAccount("123456", "00-00-00")), false)
    val addPerson = AddPerson("FirstName", Some("Middle"), "Last name", RoleWithinBusinessRelease7(Set(BeneficialShareholder)))

    val businessActivitiesCompleteModel = BusinessActivities(
      involvedInOther = Some(InvolvedInOtherNo),
      expectedBusinessTurnover = Some(ExpectedBusinessTurnover.First),
      expectedAMLSTurnover = Some(ExpectedAMLSTurnover.First),
      businessFranchise = Some(BusinessFranchiseNo),
      transactionRecord = Some(true),
      customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      ncaRegistered = Some(NCARegistered(true)),
      accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
      riskAssessmentPolicy = Some(RiskAssessmentPolicyYes(Set(PaperBased))),
      howManyEmployees = Some(HowManyEmployees(Some("5"), Some("4"))),
      identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true)),
      whoIsYourAccountant = Some(WhoIsYourAccountant("Accountant's name", Some("Accountant's trading name"),
        UkAccountantsAddress("address1", "address2", Some("address3"), Some("address4"), "POSTCODE"))),
      taxMatters = Some(TaxMatters(false)),
      transactionRecordTypes = Some(TransactionTypes(Set(Paper))),
      hasChanged = false,
      hasAccepted = true
    )

    val responsiblePeople = responsiblePersonGen.sample.get

    val tcsp = Tcsp(Some(TcspTypes(Set(
      NomineeShareholdersProvider,
      TrusteeProvider))),
      Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
      Some(true),
      None,
      hasAccepted = true
    )

    val asp = Asp(Some(ServicesOfBusiness(Set(Accountancy))), Some(OtherBusinessTaxMattersNo), false, false)

    val msb = MoneyServiceBusiness(
      throughput = Some(ExpectedThroughput.Second),
      businessUseAnIPSP = Some(BusinessUseAnIPSPYes("name", "123456789123456")),
      identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true)),
      Some(WhichCurrencies(
        Seq("USD", "GBP", "EUR"),
        usesForeignCurrencies = Some(true),
        Some(BankMoneySource("bank names")),
        Some(WholesalerMoneySource("Wholesaler Names")),
        Some(true))),
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
      fundsTransfer = Some(FundsTransfer(true)),
      branchesOrAgents = Some(BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))),
      sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
      mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
      ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
      false,
      true
    )

    val hvd = Hvd(
      cashPayment = Some(CashPaymentYes(new LocalDate(1956, 2, 15))),
      Some(Products(Set(Cars))),
      None,
      Some(HowWillYouSellGoods(Seq(Retail))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(true),
      Some(PaymentMethods(courier = true, direct = true, other = Some("foo"))),
      Some(LinkedCashPayments(false)),
      Some(DateOfChange(new LocalDate("2016-02-24"))))

    val supervision = Supervision(
      Some(AnotherBodyNo),
      Some(ProfessionalBodyMemberYes),
      Some(ProfessionalBodies(Set(AccountingTechnicians))),
      Some(SupervisionProfessionalBodyYes("details")),
      hasAccepted = true
    )

    val subscription = SubscriptionResponse(
      "bundle",
      "XDML00000000000",
      None,
      Some(true)
    )

    val amendVariationRenewalResponse = AmendVariationRenewalResponse(
      "update",
      "12345",
      115.0,
      Some(125.0),
      Some(115.0),
      0,
      None,
      240.0,
      Some("ref"),
      None
    )

    val dataImport = DataImport("test.json")

    val updateSave4LaterResponse = UpdateSave4LaterResponse(
      Some(dataImport),
      Some(viewResponse),
      Some(businessMatching),
      Some(estateAgentBusiness),
      Some(tradingPremises),
      Some(aboutTheBusiness),
      Some(Seq(bankDetails)),
      Some(addPerson),
      Some(businessActivitiesCompleteModel),
      Some(Seq(responsiblePeople)),
      Some(tcsp),
      Some(asp),
      Some(msb),
      Some(hvd),
      Some(supervision),
      Some(subscription),
      Some(amendVariationRenewalResponse))

  }

  "UpdateSave4LaterService" when {

    "update is called" must {
      "retrieve the specified file from stubs and update save for later with the contents" in new Fixture {

        mockCacheSave[ViewResponse]
        mockCacheSave[BusinessMatching]
        mockCacheSave[Seq[TradingPremises]]
        mockCacheSave[Seq[BankDetails]]
        mockCacheSave[AddPerson]
        mockCacheSave[BusinessActivities]
        mockCacheSave[Tcsp]
        mockCacheSave[Seq[ResponsiblePerson]]
        mockCacheSave[Asp]
        mockCacheSave[MoneyServiceBusiness]
        mockCacheSave[Hvd]
        mockCacheSave[Supervision]
        mockCacheSave[AboutTheBusiness]
        mockCacheSave[EstateAgentBusiness]
        mockCacheSave[SubscriptionResponse]
        mockCacheSave[AmendVariationRenewalResponse]
        mockCacheSave[DataImport]

        await(updateSave4LaterService.update(updateSave4LaterResponse))

        verify(mockCacheConnector).save[ViewResponse](eqTo(ViewResponse.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[Seq[TradingPremises]](eqTo(TradingPremises.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[Seq[BankDetails]](eqTo(BankDetails.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[AddPerson](eqTo(AddPerson.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[BusinessActivities](eqTo(BusinessActivities.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[Tcsp](eqTo(Tcsp.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[Asp](eqTo(Asp.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[Hvd](eqTo(Hvd.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[Supervision](eqTo(Supervision.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[EstateAgentBusiness](eqTo(EstateAgentBusiness.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[SubscriptionResponse](eqTo(SubscriptionResponse.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
        verify(mockCacheConnector).save[DataImport](eqTo(DataImport.key), eqTo(dataImport))(any(), any(), any())
      }
    }
  }
}
