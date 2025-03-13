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

import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models._
import models.amp.Amp
import models.asp.Service.Accountancy
import models.asp.{Asp, OtherBusinessTaxMattersNo, ServicesOfBusiness}
import models.bankdetails.BankAccountType.PersonalAccount
import models.bankdetails.{BankAccount, BankAccountIsUk, BankDetails, UKAccount}
import models.businessactivities.TransactionTypes.Paper
import models.businessactivities._
import models.businessdetails._
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7.{BeneficialShareholder, RoleWithinBusinessRelease7}
import models.eab.Eab
import models.hvd.Products.Cars
import models.hvd.SalesChannel.Retail
import models.hvd._
import models.moneyservicebusiness._
import models.responsiblepeople.ResponsiblePerson
import models.supervision.ProfessionalBodies._
import models.supervision.{ProfessionalBodyYes => SupervisionProfessionalBodyYes, _}
import models.tcsp.ProvidedServices.{Other => PSOther, PhonecallHandling}
import models.tcsp.TcspTypes._
import models.tcsp._
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{AmlsSpec, DependencyMocks}

import java.time.LocalDate

class UpdateMongoCacheServiceSpec
    extends AmlsSpec
    with BusinessMatchingGenerator
    with TradingPremisesGenerator
    with ResponsiblePersonGenerator {

  trait Fixture extends DependencyMocks {

    val http: HttpClientV2      = mock[HttpClientV2]
    val updateMongoCacheService = new UpdateMongoCacheService(http, mockCacheConnector, appConfig)

    val credId = "12341234"

    val viewResponse: ViewResponse = ViewResponse(
      etmpFormBundleNumber = "FORMBUNDLENUMBER",
      businessMatchingSection = BusinessMatching(),
      eabSection = None,
      tradingPremisesSection = None,
      businessDetailsSection = None,
      bankDetailsSection = Seq(None),
      aboutYouSection = AddPerson(
        "FirstName",
        None,
        "LastName",
        RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))
      ),
      businessActivitiesSection = None,
      responsiblePeopleSection = None,
      tcspSection = None,
      aspSection = None,
      msbSection = None,
      hvdSection = None,
      ampSection = None,
      supervisionSection = None
    )

    val businessMatching: BusinessMatching = businessMatchingGen.sample.get

    val completeServiceList: Seq[String] = Seq("auctioneering", "residential")

    val completeServices: JsObject = Json.obj("eabServicesProvided" -> completeServiceList)

    val completeDateOfChange: JsObject = Json.obj(
      "dateOfChange" -> "2019-01-01"
    )

    val completeEstateAgencyActPenalty: JsObject = Json.obj(
      "penalisedEstateAgentsAct"       -> true,
      "penalisedEstateAgentsActDetail" -> "details"
    )

    val completePenalisedProfessionalBody: JsObject = Json.obj(
      "penalisedProfessionalBody"       -> true,
      "penalisedProfessionalBodyDetail" -> "details"
    )

    val completeRedressScheme: JsObject = Json.obj(
      "redressScheme"       -> "propertyRedressScheme",
      "redressSchemeDetail" -> "null"
    )

    val completeMoneyProtectionScheme: JsObject = Json.obj(
      "clientMoneyProtectionScheme" -> true
    )

    val completeData: JsObject = completeServices ++
      completeDateOfChange ++
      completeEstateAgencyActPenalty ++
      completePenalisedProfessionalBody ++
      completeRedressScheme ++
      completeMoneyProtectionScheme

    val estateAgentBusiness: Eab = Eab(completeData, hasAccepted = true)

    val tradingPremises: Seq[TradingPremises] = Seq(tradingPremisesGen.sample.get, tradingPremisesGen.sample.get)

    val businessDetails: BusinessDetails = BusinessDetails(
      previouslyRegistered = Some(PreviouslyRegisteredYes(Some("12345678"))),
      activityStartDate = Some(ActivityStartDate(LocalDate.of(1990, 2, 24))),
      vatRegistered = Some(VATRegisteredYes("123456789")),
      corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1234567890")),
      contactingYou = Some(ContactingYou(Some("1234567890"), Some("test@test.com"))),
      registeredOffice = Some(RegisteredOfficeUK("38B", None, None, None, "AA1 1AA")),
      altCorrespondenceAddress = Some(true),
      correspondenceAddress = Some(
        CorrespondenceAddress(
          Some(
            CorrespondenceAddressUk(
              "Name",
              "Business Name",
              "address 1",
              Some("address 2"),
              Some("address 3"),
              Some("address 4"),
              "AA11 1AA"
            )
          ),
          None
        )
      ),
      hasAccepted = true
    )

    val ukBankAccount: BankAccount =
      BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("123456", "00-00-00")))

    val bankDetails: BankDetails = BankDetails(Some(PersonalAccount), None, Some(ukBankAccount), false)
    val addPerson: AddPerson     =
      AddPerson("FirstName", Some("Middle"), "Last name", RoleWithinBusinessRelease7(Set(BeneficialShareholder)))

    val businessActivitiesCompleteModel: BusinessActivities = BusinessActivities(
      involvedInOther = Some(InvolvedInOtherNo),
      expectedBusinessTurnover = Some(ExpectedBusinessTurnover.First),
      expectedAMLSTurnover = Some(ExpectedAMLSTurnover.First),
      businessFranchise = Some(BusinessFranchiseNo),
      transactionRecord = Some(true),
      customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      ncaRegistered = Some(NCARegistered(true)),
      accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
      riskAssessmentPolicy =
        Some(RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased)))),
      howManyEmployees = Some(HowManyEmployees(Some("5"), Some("4"))),
      identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true)),
      whoIsYourAccountant = Some(
        WhoIsYourAccountant(
          Some(WhoIsYourAccountantName("Accountant's name", Some("Accountant's trading name"))),
          Some(WhoIsYourAccountantIsUk(true)),
          Some(UkAccountantsAddress("address1", Some("address2"), Some("address3"), Some("address4"), "POSTCODE"))
        )
      ),
      taxMatters = Some(TaxMatters(false)),
      transactionRecordTypes = Some(TransactionTypes(Set(Paper))),
      hasChanged = false,
      hasAccepted = true
    )

    val responsiblePeople: ResponsiblePerson = responsiblePersonGen.sample.get

    val tcsp: Tcsp = Tcsp(
      Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider))),
      None,
      None,
      Some(ProvidedServices(Set(PhonecallHandling, PSOther("other service")))),
      Some(true),
      None,
      hasAccepted = true
    )

    val asp: Asp = Asp(Some(ServicesOfBusiness(Set(Accountancy))), Some(OtherBusinessTaxMattersNo), false, false)

    val msb: MoneyServiceBusiness = MoneyServiceBusiness(
      throughput = Some(ExpectedThroughput.Second),
      businessUseAnIPSP = Some(BusinessUseAnIPSPYes("name", "123456789123456")),
      identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true)),
      Some(
        WhichCurrencies(
          Seq("USD", "GBP", "EUR"),
          Some(UsesForeignCurrenciesYes),
          Some(
            MoneySources(
              Some(BankMoneySource("bank names")),
              Some(WholesalerMoneySource("Wholesaler Names")),
              Some(true)
            )
          )
        )
      ),
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
      fundsTransfer = Some(FundsTransfer(true)),
      branchesOrAgents = Some(
        BranchesOrAgents(
          BranchesOrAgentsHasCountries(true),
          Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB"))))
        )
      ),
      sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
      ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
      fxTransactionsInNext12Months = Some(FXTransactionsInNext12Months("12345678963")),
      false,
      true
    )

    val hvd: Hvd = Hvd(
      cashPayment =
        Some(CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(LocalDate.of(1956, 2, 15))))),
      Some(Products(Set(Cars))),
      None,
      Some(HowWillYouSellGoods(Set(Retail))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(true),
      Some(PaymentMethods(courier = true, direct = true, other = Some("foo"))),
      Some(LinkedCashPayments(false)),
      Some(DateOfChange(LocalDate.of(2016, 2, 24)))
    )

    val ampData: JsObject = Json.obj(
      "typeOfParticipant"            -> Seq("artGalleryOwner"),
      "soldOverThreshold"            -> true,
      "dateTransactionOverThreshold" -> LocalDate.now.toString,
      "identifyLinkedTransactions"   -> true,
      "percentageExpectedTurnover"   -> "fortyOneToSixty"
    )

    val amp: Amp = Amp(data = ampData)

    val supervision: Supervision = Supervision(
      Some(AnotherBodyNo),
      Some(ProfessionalBodyMemberYes),
      Some(ProfessionalBodies(Set(AccountingTechnicians))),
      Some(SupervisionProfessionalBodyYes("details")),
      hasAccepted = true
    )

    val subscription: SubscriptionResponse = SubscriptionResponse(
      "bundle",
      "XDML00000000000",
      None,
      Some(true)
    )

    val amendVariationRenewalResponse: AmendVariationRenewalResponse = AmendVariationRenewalResponse(
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

    val dataImport: DataImport = DataImport("test.json")

    val updateMongoCacheResponse: UpdateMongoCacheResponse = UpdateMongoCacheResponse(
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
      Some(amendVariationRenewalResponse)
    )

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

        verify(mockCacheConnector).save[ViewResponse](any(), eqTo(ViewResponse.key), any())(any())
        verify(mockCacheConnector).save[BusinessMatching](any(), eqTo(BusinessMatching.key), any())(any())
        verify(mockCacheConnector).save[Seq[TradingPremises]](any(), eqTo(TradingPremises.key), any())(any())
        verify(mockCacheConnector).save[Seq[BankDetails]](any(), eqTo(BankDetails.key), any())(any())
        verify(mockCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any())
        verify(mockCacheConnector).save[BusinessActivities](any(), eqTo(BusinessActivities.key), any())(any())
        verify(mockCacheConnector).save[Tcsp](any(), eqTo(Tcsp.key), any())(any())
        verify(mockCacheConnector).save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), any())(any())
        verify(mockCacheConnector).save[Asp](any(), eqTo(Asp.key), any())(any())
        verify(mockCacheConnector).save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any())
        verify(mockCacheConnector).save[Hvd](any(), eqTo(Hvd.key), any())(any())
        verify(mockCacheConnector).save[Supervision](any(), eqTo(Supervision.key), any())(any())
        verify(mockCacheConnector).save[BusinessDetails](any(), eqTo(BusinessDetails.key), any())(any())
        verify(mockCacheConnector).save[Eab](any(), eqTo(Eab.key), any())(any())
        verify(mockCacheConnector).save[SubscriptionResponse](any(), eqTo(SubscriptionResponse.key), any())(any())
        verify(mockCacheConnector)
          .save[AmendVariationRenewalResponse](any(), eqTo(AmendVariationRenewalResponse.key), any())(any())
        verify(mockCacheConnector).save[DataImport](any(), eqTo(DataImport.key), eqTo(dataImport))(any())
      }
    }
  }
}
