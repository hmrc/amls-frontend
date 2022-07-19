/*
 * Copyright 2022 HM Revenue & Customs
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

import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.amp.Amp
import models.asp.{Accountancy, Asp, OtherBusinessTaxMattersNo, ServicesOfBusiness}
import models.bankdetails.{BankAccount, BankAccountIsUk, BankDetails, PersonalAccount, UKAccount}
import models.businessactivities._
import models.businessdetails._
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7.{BeneficialShareholder, RoleWithinBusinessRelease7}
import models.eab.Eab
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
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpClient
import utils.{AmlsSpec, DependencyMocks}

import scala.collection.Seq


class UpdateMongoCacheServiceSpec extends AmlsSpec
  with BusinessMatchingGenerator
  with TradingPremisesGenerator
  with ResponsiblePersonGenerator {

  trait Fixture extends DependencyMocks {

    val http = mock[HttpClient]
    val updateMongoCacheService = new UpdateMongoCacheService(http, mockCacheConnector, appConfig)

    val credId = "12341234"

    val viewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = None,
      tradingPremisesSection = None,
      businessDetailsSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))),
      businessActivitiesSection = None,
      responsiblePeopleSection = None,
      tcspSection = None,
      aspSection = None,
      msbSection = None,
      hvdSection = None,
      ampSection = None,
      supervisionSection = None
    )

    val businessMatching = businessMatchingGen.sample.get

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

    val estateAgentBusiness = Eab(completeData,  hasAccepted = true)

    val tradingPremises = Seq(tradingPremisesGen.sample.get, tradingPremisesGen.sample.get)


    val businessDetails = BusinessDetails(
      previouslyRegistered = Some(PreviouslyRegisteredYes(Some("12345678"))),
      activityStartDate = Some(ActivityStartDate(new LocalDate(1990, 2, 24))),
      vatRegistered = Some(VATRegisteredYes("123456789")),
      corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1234567890")),
      contactingYou = Some(ContactingYou(Some("1234567890"), Some("test@test.com"))),
      registeredOffice = Some(RegisteredOfficeUK("38B", "line2", None, None, "AA1 1AA")),
      altCorrespondenceAddress = Some(true),
      correspondenceAddress = Some(CorrespondenceAddress(Some(CorrespondenceAddressUk("Name",
        "Business Name",
        "address 1",
        "address 2",
        Some("address 3"),
        Some("address 4"),
        "AA11 1AA")), None)),
      hasAccepted = true
    )

    val ukBankAccount = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("123456", "00-00-00")))

    val bankDetails = BankDetails(Some(PersonalAccount), None, Some(ukBankAccount), false)
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
      riskAssessmentPolicy = Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased)))),
      howManyEmployees = Some(HowManyEmployees(Some("5"), Some("4"))),
      identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true)),
      whoIsYourAccountant = Some(WhoIsYourAccountant(
        Some(WhoIsYourAccountantName("Accountant's name", Some("Accountant's trading name"))),
        Some(WhoIsYourAccountantIsUk(true)),
        Some(UkAccountantsAddress("address1", "address2", Some("address3"), Some("address4"), "POSTCODE")))),
      taxMatters = Some(TaxMatters(false)),
      transactionRecordTypes = Some(TransactionTypes(Set(Paper))),
      hasChanged = false,
      hasAccepted = true
    )

    val responsiblePeople = responsiblePersonGen.sample.get

    val tcsp = Tcsp(
      Some(TcspTypes(Set(
        NomineeShareholdersProvider,
        TrusteeProvider))),
      None,
      None,
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
        Some(UsesForeignCurrenciesYes),
        Some(MoneySources(Some(BankMoneySource("bank names")),
          Some(WholesalerMoneySource("Wholesaler Names")),
          Some(true)))
      )),
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
      fundsTransfer = Some(FundsTransfer(true)),
      branchesOrAgents = Some(BranchesOrAgents(BranchesOrAgentsHasCountries(true), Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB")))))),
      sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
      ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
      fxTransactionsInNext12Months = Some(FXTransactionsInNext12Months("12345678963")),
      false,
      true
    )

    val hvd = Hvd(
      cashPayment = Some(CashPayment(
        CashPaymentOverTenThousandEuros(true),
        Some(CashPaymentFirstDate(new LocalDate(1956, 2, 15))))),
      Some(Products(Set(Cars))),
      None,
      Some(HowWillYouSellGoods(Set(Retail))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(true),
      Some(PaymentMethods(courier = true, direct = true, other = Some("foo"))),
      Some(LinkedCashPayments(false)),
      Some(DateOfChange(new LocalDate("2016-02-24"))))

    val ampData = Json.obj(
      "typeOfParticipant"     -> Seq("artGalleryOwner"),
      "soldOverThreshold"     -> true,
      "dateTransactionOverThreshold"  -> LocalDate.now.toString,
      "identifyLinkedTransactions"    -> true,
      "percentageExpectedTurnover"    -> "fortyOneToSixty"
    )

    val amp = Amp(data = ampData)

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
      None,
      None,
      0,
      None,
      240.0,
      Some("ref"),
      None
    )

    val dataImport = DataImport("test.json")

    val updateMongoCacheResponse = UpdateMongoCacheResponse(
      Some(dataImport),
      Some(viewResponse),
      Some(businessMatching),
      Some(estateAgentBusiness),
      Some(tradingPremises),
      Some(businessDetails),
      Some(Seq(bankDetails)),
      Some(addPerson),
      Some(businessActivitiesCompleteModel),
      Some(Seq(responsiblePeople)),
      Some(tcsp),
      Some(asp),
      Some(msb),
      Some(hvd),
      Some(amp),
      Some(supervision),
      Some(subscription),
      Some(amendVariationRenewalResponse))

  }

  "UpdateMongoCacheService" when {

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
        mockCacheSave[BusinessDetails]
        mockCacheSave[Eab]
        mockCacheSave[SubscriptionResponse]
        mockCacheSave[AmendVariationRenewalResponse]
        mockCacheSave[DataImport]

        await(updateMongoCacheService.update(credId, updateMongoCacheResponse))

        verify(mockCacheConnector).save[ViewResponse](any(), eqTo(ViewResponse.key), any())(any(), any())
        verify(mockCacheConnector).save[BusinessMatching](any(), eqTo(BusinessMatching.key), any())(any(), any())
        verify(mockCacheConnector).save[Seq[TradingPremises]](any(), eqTo(TradingPremises.key), any())(any(), any())
        verify(mockCacheConnector).save[Seq[BankDetails]](any(), eqTo(BankDetails.key), any())(any(), any())
        verify(mockCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any(), any())
        verify(mockCacheConnector).save[BusinessActivities](any(), eqTo(BusinessActivities.key), any())(any(), any())
        verify(mockCacheConnector).save[Tcsp](any(), eqTo(Tcsp.key), any())(any(), any())
        verify(mockCacheConnector).save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), any())(any(), any())
        verify(mockCacheConnector).save[Asp](any(), eqTo(Asp.key), any())(any(), any())
        verify(mockCacheConnector).save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any(), any())
        verify(mockCacheConnector).save[Hvd](any(), eqTo(Hvd.key), any())(any(), any())
        verify(mockCacheConnector).save[Supervision](any(), eqTo(Supervision.key), any())(any(), any())
        verify(mockCacheConnector).save[BusinessDetails](any(), eqTo(BusinessDetails.key), any())(any(), any())
        verify(mockCacheConnector).save[Eab](any(), eqTo(Eab.key), any())(any(), any())
        verify(mockCacheConnector).save[SubscriptionResponse](any(), eqTo(SubscriptionResponse.key), any())(any(), any())
        verify(mockCacheConnector).save[AmendVariationRenewalResponse](any(), eqTo(AmendVariationRenewalResponse.key), any())(any(), any())
        verify(mockCacheConnector).save[DataImport](any(), eqTo(DataImport.key), eqTo(dataImport))(any(), any())
      }
    }
  }
}
