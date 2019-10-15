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

package controllers

import connectors.DataCacheConnector
import controllers.actions.{SuccessfulAuthAction, SuccessfulAuthActionNoAmlsRefNo}
import generators.StatusGenerator
import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.BusinessDetails
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Renewal
import models.responsiblepeople.TimeAtAddress.OneToThreeYears
import models.responsiblepeople._
import models.status._
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{status => _, _}
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsResultException
import play.api.mvc.{BodyParsers, MessagesActionBuilder, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.{ExecutionContext, Future}

class LandingControllerWithAmendmentsSpec extends AmlsSpec with MockitoSugar with MustMatchers with StatusGenerator {

  val businessCustomerUrl = "TestUrl"
  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier()

  trait Fixture { self =>

    val completeResponsiblePerson: ResponsiblePerson = ResponsiblePerson(
      personName = Some(PersonName("ANSTY", Some("EMIDLLE"), "DAVID")),
      legalName = Some(PreviousName(Some(false), None, None, None)),
      legalNameChangeDate = None,
      knownBy = Some(KnownBy(Some(false), None)),
      personResidenceType = Some(PersonResidenceType(NonUKResidence, Some(Country("Antigua and Barbuda", "bb")), Some(Country("United Kingdom", "GB")))),
      ukPassport = Some(UKPassportNo),
      nonUKPassport = Some(NoPassport),
      dateOfBirth = Some(DateOfBirth(LocalDate.parse("2000-01-01"))),
      contactDetails = Some(ContactDetails("0912345678", "TEST@EMAIL.COM")),
      addressHistory = Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(PersonAddressUK("add1", "add2", Some("add3"), Some("add4"), "de4 5tg"), Some(OneToThreeYears), None)), None, None)),
      positions = Some(Positions(Set(NominatedOfficer, SoleProprietor), Some(PositionStartDate(new LocalDate(2002, 2, 2))))),
      saRegistered = Some(SaRegisteredNo),
      vatRegistered = Some(VATRegisteredNo),
      experienceTraining = Some(ExperienceTrainingNo),
      training = Some(TrainingNo),
      approvalFlags = ApprovalFlags(Some(true), Some(true)),
      hasChanged = false,
      hasAccepted = true,
      lineId = Some(2),
      status = None,
      endDate = None,
      soleProprietorOfAnotherBusiness = None
    )

    val request = addToken(authRequest)

    val controller = new LandingController(
      enrolmentsService = mock[AuthEnrolmentsService],
      landingService = mock[LandingService],
      authAction = SuccessfulAuthAction,
      auditConnector = mock[AuditConnector],
      cacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      ds = commonDependencies,
      mcc = mockMcc,
      messagesApi = messagesApi,
      parser = mock[BodyParsers.Default])

    when(controller.landingService.refreshCache(any(), any[String](), any())(any(), any()))
      .thenReturn(Future.successful(mock[CacheMap]))

    when {
      controller.landingService.setAltCorrespondenceAddress(any(), any[String])(any(), any())
    } thenReturn Future.successful(mock[CacheMap])

    val completeATB = mock[BusinessDetails]

    val emptyCacheMap = CacheMap("test", Map.empty)

    def setUpMocksForNoDataInSaveForLater(controller: LandingController) = {
      when(controller.landingService.cacheMap(any[String])(any(), any()))
        .thenReturn(Future.successful(None))
    }

    def setUpMocksForDataExistsInKeystore(controller: LandingController) = {
      val reviewDetails = ReviewDetails(
        "Business Name",
        None,
        Address("Line1", "Line2", None, None, Some("AA11AA"), Country("United Kingdom", "UK")),
        "testSafeId")

      when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
        .thenReturn(Future.successful(Some(reviewDetails)))

      reviewDetails
    }

    def setUpMocksForNoDataInKeyStore(controller: LandingController) = {
      when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
        .thenReturn(Future.successful(None))
    }

    def setUpMocksForDataExistsInSaveForLater(controller: LandingController, testData: CacheMap = mock[CacheMap]) = {
      when(controller.landingService.cacheMap(any[String])(any(), any()))
        .thenReturn(Future.successful(Some(testData)))
    }

    //noinspection ScalaStyle
    def buildTestCacheMap(hasChanged: Boolean,
                          includesResponse: Boolean,
                          noTP: Boolean = false,
                          noRP: Boolean = false,
                          includeSubmissionStatus: Boolean = false,
                          includeDataImport: Boolean = false,
                          cacheMap: CacheMap = mock[CacheMap]): CacheMap = {

      val testASP = Asp(hasChanged = hasChanged)
      val testAMP = Amp("someid", hasChanged = hasChanged)
      val testBusinessDetails = BusinessDetails(hasChanged = hasChanged)
      val testBankDetails = Seq(BankDetails(hasChanged = hasChanged))
      val testBusinessActivities = BusinessActivities(hasChanged = hasChanged)
      val testBusinessMatching = BusinessMatching(hasChanged = hasChanged)
      val testEstateAgentBusiness = EstateAgentBusiness(hasChanged = hasChanged)
      val testMoneyServiceBusiness = MoneyServiceBusiness(hasChanged = hasChanged)
      val testResponsiblePeople = Seq(ResponsiblePerson(hasChanged = hasChanged))
      val testSupervision = Supervision(hasChanged = hasChanged)
      val testTcsp = Tcsp(hasChanged = hasChanged)
      val testTradingPremises = Seq(TradingPremises(hasChanged = hasChanged))
      val testHvd = Hvd(hasChanged = hasChanged)
      val testRenewal = Renewal(hasChanged = hasChanged)

      when(cacheMap.getEntry[Asp](Asp.key)).thenReturn(Some(testASP))
      when(cacheMap.getEntry[Amp](Amp.key)).thenReturn(Some(testAMP))
      when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(testBusinessDetails))
      when(cacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any())).thenReturn(Some(testBankDetails))
      when(cacheMap.getEntry[BusinessActivities](BusinessActivities.key)).thenReturn(Some(testBusinessActivities))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(testBusinessMatching))
      when(cacheMap.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)).thenReturn(Some(testEstateAgentBusiness))
      when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)).thenReturn(Some(testMoneyServiceBusiness))
      when(cacheMap.getEntry[Supervision](Supervision.key)).thenReturn(Some(testSupervision))
      when(cacheMap.getEntry[Tcsp](Tcsp.key)).thenReturn(Some(testTcsp))
      when(cacheMap.getEntry[Hvd](Hvd.key)).thenReturn(Some(testHvd))
      when(cacheMap.getEntry[Renewal](Renewal.key)).thenReturn(Some(testRenewal))

      when(cacheMap.getEntry[DataImport](DataImport.key)).thenReturn(if (includeDataImport)
        Some(DataImport("test.json"))
      else
        None)

      val submissionRequestStatus = if (includeSubmissionStatus) Some(SubmissionRequestStatus(true)) else None
      when(cacheMap.getEntry[SubmissionRequestStatus](SubmissionRequestStatus.key)).thenReturn(submissionRequestStatus)

      if (noTP) {
        when(cacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())) thenThrow new JsResultException(Seq.empty)
      } else {
        when(cacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())).thenReturn(Some(testTradingPremises))
      }

      if (noRP) {
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())) thenThrow new JsResultException(Seq.empty)
      } else {
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(Some(testResponsiblePeople))
      }

      if (includesResponse) {
        val testResponse = SubscriptionResponse(
          etmpFormBundleNumber = "TESTFORMBUNDLENUMBER",
          amlsRefNo = "TESTAMLSREFNNO", subscriptionFees = Some(SubscriptionFees(
            paymentReference = "TESTPAYMENTREF",
            registrationFee = 100.45,
            fpFee = None,
            fpFeeRate = None,
            approvalCheckFee = None,
            approvalCheckFeeRate = None,
            premiseFee = 123.78,
            premiseFeeRate = None,
            totalFees = 17623.76
          ))
        )

        when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(testResponse))

      }

      cacheMap
    }
  }

  trait FixtureNoAmlsNumber extends AuthorisedFixture { self =>

    val completeResponsiblePerson: ResponsiblePerson = ResponsiblePerson(
      personName = Some(PersonName("ANSTY", Some("EMIDLLE"), "DAVID")),
      legalName = Some(PreviousName(Some(false), None, None, None)),
      legalNameChangeDate = None,
      knownBy = Some(KnownBy(Some(false), None)),
      personResidenceType = Some(PersonResidenceType(NonUKResidence, Some(Country("Antigua and Barbuda", "bb")), Some(Country("United Kingdom", "GB")))),
      ukPassport = Some(UKPassportNo),
      nonUKPassport = Some(NoPassport),
      dateOfBirth = Some(DateOfBirth(LocalDate.parse("2000-01-01"))),
      contactDetails = Some(ContactDetails("0912345678", "TEST@EMAIL.COM")),
      addressHistory = Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(PersonAddressUK("add1", "add2", Some("add3"), Some("add4"), "de4 5tg"), Some(OneToThreeYears), None)), None, None)),
      positions = Some(Positions(Set(NominatedOfficer, SoleProprietor), Some(PositionStartDate(new LocalDate(2002, 2, 2))))),
      saRegistered = Some(SaRegisteredNo),
      vatRegistered = Some(VATRegisteredNo),
      experienceTraining = Some(ExperienceTrainingNo),
      training = Some(TrainingNo),
      approvalFlags = ApprovalFlags(Some(true), Some(true)),
      hasChanged = false,
      hasAccepted = true,
      lineId = Some(2),
      status = None,
      endDate = None,
      soleProprietorOfAnotherBusiness = None
    )

    val request = addToken(authRequest)

    val controller = new LandingController(
      enrolmentsService = mock[AuthEnrolmentsService],
      landingService = mock[LandingService],
      authAction = SuccessfulAuthActionNoAmlsRefNo,
      auditConnector = mock[AuditConnector],
      cacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      ds = commonDependencies,
      mcc = mockMcc,
      messagesApi = messagesApi,
      parser = mock[BodyParsers.Default])

    when(controller.landingService.refreshCache(any(), any[String](), any())(any(), any()))
      .thenReturn(Future.successful(mock[CacheMap]))

    when {
      controller.landingService.setAltCorrespondenceAddress(any(), any[String])(any(), any())
    } thenReturn Future.successful(mock[CacheMap])

    val completeATB = mock[BusinessDetails]

    val emptyCacheMap = CacheMap("test", Map.empty)

    def setUpMocksForNoDataInSaveForLater(controller: LandingController) = {
      when(controller.landingService.cacheMap(any[String])(any(), any()))
        .thenReturn(Future.successful(None))
    }

    def setUpMocksForDataExistsInKeystore(controller: LandingController) = {
      val reviewDetails = ReviewDetails(
        "Business Name",
        None,
        Address("Line1", "Line2", None, None, Some("AA11AA"), Country("United Kingdom", "UK")),
        "testSafeId")

      when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
        .thenReturn(Future.successful(Some(reviewDetails)))

      reviewDetails
    }

    def setUpMocksForNoDataInKeyStore(controller: LandingController) = {
      when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
        .thenReturn(Future.successful(None))
    }

    def setUpMocksForDataExistsInSaveForLater(controller: LandingController, testData: CacheMap = mock[CacheMap]) = {
      when(controller.landingService.cacheMap(any[String])(any(), any()))
        .thenReturn(Future.successful(Some(testData)))
    }

    //noinspection ScalaStyle
    def buildTestCacheMap(hasChanged: Boolean,
                          includesResponse: Boolean,
                          noTP: Boolean = false,
                          noRP: Boolean = false,
                          includeSubmissionStatus: Boolean = false,
                          includeDataImport: Boolean = false,
                          cacheMap: CacheMap = mock[CacheMap]): CacheMap = {

      val testASP = Asp(hasChanged = hasChanged)
      val testBusinessDetails = BusinessDetails(hasChanged = hasChanged)
      val testBankDetails = Seq(BankDetails(hasChanged = hasChanged))
      val testBusinessActivities = BusinessActivities(hasChanged = hasChanged)
      val testBusinessMatching = BusinessMatching(hasChanged = hasChanged)
      val testEstateAgentBusiness = EstateAgentBusiness(hasChanged = hasChanged)
      val testMoneyServiceBusiness = MoneyServiceBusiness(hasChanged = hasChanged)
      val testResponsiblePeople = Seq(ResponsiblePerson(hasChanged = hasChanged))
      val testSupervision = Supervision(hasChanged = hasChanged)
      val testTcsp = Tcsp(hasChanged = hasChanged)
      val testTradingPremises = Seq(TradingPremises(hasChanged = hasChanged))
      val testHvd = Hvd(hasChanged = hasChanged)
      val testRenewal = Renewal(hasChanged = hasChanged)

      when(cacheMap.getEntry[Asp](Asp.key)).thenReturn(Some(testASP))
      when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(testBusinessDetails))
      when(cacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any())).thenReturn(Some(testBankDetails))
      when(cacheMap.getEntry[BusinessActivities](BusinessActivities.key)).thenReturn(Some(testBusinessActivities))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(testBusinessMatching))
      when(cacheMap.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)).thenReturn(Some(testEstateAgentBusiness))
      when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)).thenReturn(Some(testMoneyServiceBusiness))
      when(cacheMap.getEntry[Supervision](Supervision.key)).thenReturn(Some(testSupervision))
      when(cacheMap.getEntry[Tcsp](Tcsp.key)).thenReturn(Some(testTcsp))
      when(cacheMap.getEntry[Hvd](Hvd.key)).thenReturn(Some(testHvd))
      when(cacheMap.getEntry[Renewal](Renewal.key)).thenReturn(Some(testRenewal))

      when(cacheMap.getEntry[DataImport](DataImport.key)).thenReturn(if (includeDataImport)
        Some(DataImport("test.json"))
      else
        None)

      val submissionRequestStatus = if (includeSubmissionStatus) Some(SubmissionRequestStatus(true)) else None
      when(cacheMap.getEntry[SubmissionRequestStatus](SubmissionRequestStatus.key)).thenReturn(submissionRequestStatus)

      if (noTP) {
        when(cacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())) thenThrow new JsResultException(Seq.empty)
      } else {
        when(cacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())).thenReturn(Some(testTradingPremises))
      }

      if (noRP) {
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())) thenThrow new JsResultException(Seq.empty)
      } else {
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(Some(testResponsiblePeople))
      }

      if (includesResponse) {
        val testResponse = SubscriptionResponse(
          etmpFormBundleNumber = "TESTFORMBUNDLENUMBER",
          amlsRefNo = "TESTAMLSREFNNO", subscriptionFees = Some(SubscriptionFees(
            paymentReference = "TESTPAYMENTREF",
            registrationFee = 100.45,
            fpFee = None,
            fpFeeRate = None,
            approvalCheckFee = None,
            approvalCheckFeeRate = None,
            premiseFee = 123.78,
            premiseFeeRate = None,
            totalFees = 17623.76
          ))
        )

        when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(testResponse))

      }

      cacheMap
    }
  }

  "show landing page without authorisation" in new Fixture {
    val result = controller.start()(FakeRequest().withSession())
    status(result) mustBe OK
  }

  "show the landing page when authorised, but 'redirect' = false" in new Fixture {
    val result = controller.start(false)(request)
    status(result) mustBe OK
  }

  "direct to the service when authorised" in new Fixture {
    val result = controller.start()(request)
    status(result) must be(SEE_OTHER)
  }

  "Landing Controller" when {

    "redirect to status page" when {
      "submission status is DeRegistered and responsible person is not complete" in new FixtureNoAmlsNumber {
        val inCompleteResponsiblePeople: ResponsiblePerson = completeResponsiblePerson.copy(
          dateOfBirth = None)

        val cacheMap: CacheMap = mock[CacheMap]
        val complete: BusinessMatching = mock[BusinessMatching]

        when(complete.isComplete) thenReturn true
        when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
        when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(Some(Seq(inCompleteResponsiblePeople)))
        when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0)))))

        when(controller.landingService.cacheMap(any[String])(any(), any())) thenReturn Future.successful(Some(cacheMap))
        when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
          .thenReturn(Future.successful(rejectedStatusGen.sample.get, None))

        val result = controller.get()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
      }
    }

    "redirect to login event page" when {
      "responsible persons is not complete" in new FixtureNoAmlsNumber {
        val inCompleteResponsiblePeople: ResponsiblePerson = completeResponsiblePerson.copy(
          dateOfBirth = None
        )
        val cacheMap: CacheMap = mock[CacheMap]

        val complete: BusinessMatching = mock[BusinessMatching]

        when(complete.isComplete) thenReturn true
        when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
        when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(Some(Seq(inCompleteResponsiblePeople)))
        when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0)))))

        when(controller.landingService.cacheMap(any[String])(any(), any())) thenReturn Future.successful(Some(cacheMap))
        when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
          .thenReturn(Future.successful(activeStatusGen.sample.get, None))

        val result = controller.get()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.LoginEventController.get().url)
      }
    }

    "an enrolment exists and" when {
      "there is data in S4L and" when {
        "the Save 4 Later data does not contain any sections" when {
          "data has not changed" should {
            "refresh from API5 and redirect to status controller" in new Fixture {
              setUpMocksForDataExistsInSaveForLater(controller, emptyCacheMap)

              when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
                .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

              when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
                .thenReturn(Future.successful(NotCompleted, None))

              val result = controller.get()(request)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
              verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
            }
          }
        }

        "data has just been imported" should {

          def runImportTest(hasChanged: Boolean): Unit = new Fixture {
            val testCacheMap = buildTestCacheMap(
              hasChanged = hasChanged,
              includesResponse = false,
              includeSubmissionStatus = true,
              includeDataImport = true)

            setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

            when(testCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(Some(List()))
            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))
            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
              .thenReturn(Future.successful(NotCompleted, None))

            val result = controller.get()(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
            verify(controller.landingService, never).refreshCache(any[String](), any(), any())(any(), any())
          }

          "redirect to the status page without refreshing the cache" when {
            "hasChanged is false" in {
              runImportTest(hasChanged = false)
            }

            "hasChanged is true" in new Fixture {
              runImportTest(hasChanged = true)
            }
          }
        }

        "data has changed and" when {
          "the user has just submitted" when {
            "there are no incomplete responsible people" should {
              "refresh from API5 and redirect to status controller" in new Fixture {
                val testCacheMap = buildTestCacheMap(
                  hasChanged = true,
                  includesResponse = false,
                  includeSubmissionStatus = true)

                setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
                  .thenReturn(Future.successful(NotCompleted, None))

                when(testCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
                  .thenReturn(Some(List()))

                val result = controller.get()(requestWithHeaders("test-context" -> "ESCS"))

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))

                verify(controller.landingService).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
              }
            }

            "there are incomplete responsible people" should {
              "refresh from API5 and redirect to login events controller" in new Fixture {
                val inCompleteResponsiblePeople: ResponsiblePerson = completeResponsiblePerson.copy(
                  dateOfBirth = None
                )

                val testCacheMap = buildTestCacheMap(
                  hasChanged = true,
                  includesResponse = false,
                  includeSubmissionStatus = true)

                setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
                  .thenReturn(Future.successful(NotCompleted, None))

                when(testCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
                  .thenReturn(Some(Seq(inCompleteResponsiblePeople)))

                val result = controller.get()(requestWithHeaders(("test-context" -> "ESCS")))

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.LoginEventController.get().url))

                verify(controller.landingService).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
              }
            }
          }

          "the user has not just submitted" when {
            "there are no incomplete responsible people" should {
              "redirect to status controller without refreshing API5" in new Fixture {
                val testCacheMap = buildTestCacheMap(
                  hasChanged = true,
                  includesResponse = false)

                when {
                  controller.landingService.setAltCorrespondenceAddress(any(), any(), any(), any())(any(), any())
                } thenReturn Future.successful(testCacheMap)

                setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
                  .thenReturn(Future.successful(NotCompleted, None))

                when(testCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
                  .thenReturn(Some(List()))

                val result = controller.get()(request)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))

                verify(controller.landingService, never()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
              }
            }

            "there are incomplete responsible people" should {
              "redirect to login events" in new Fixture {
                val inCompleteResponsiblePeople: ResponsiblePerson = completeResponsiblePerson.copy(
                  dateOfBirth = None)

                val testCacheMap = buildTestCacheMap(
                  hasChanged = true,
                  includesResponse = false)

                when {
                  controller.landingService.setAltCorrespondenceAddress(any(), any(), any(), any())(any(), any())
                } thenReturn Future.successful(testCacheMap)

                setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
                  .thenReturn(Future.successful(NotCompleted, None))

                when(testCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
                  .thenReturn(Some(Seq(inCompleteResponsiblePeople)))

                val result = controller.get()(request)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.LoginEventController.get().url))

                verify(controller.landingService, never()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
              }
            }
          }
        }

        "data has not changed" should {
          "refresh from API5 and redirect to status controller" in new Fixture {
            setUpMocksForDataExistsInSaveForLater(controller, buildTestCacheMap(hasChanged = false, includesResponse = false))

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
              .thenReturn(Future.successful(NotCompleted, None))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
          }

          "refresh from API5 and redirect to status controller with duplicate submission flag set" in new Fixture {
            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", None, Some(true)))))

            val testCacheMap = buildTestCacheMap(hasChanged = false, includesResponse = false)
            setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)
            when(testCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(None)
            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
              .thenReturn(Future.successful(NotCompleted, None))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get(true).url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
          }

          "refresh from API5 and redirect to status controller when there is no TP or RP data" in new Fixture {
            val testCacheMap = buildTestCacheMap(hasChanged = false, includesResponse = false, noTP = true, noRP = true)

            setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

            val fixedCacheMap = buildTestCacheMap(hasChanged = false, includesResponse = false)

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", None))))

            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
              .thenReturn(Future.successful(NotCompleted, None))

            when(fixedCacheMap.getEntry[SubscriptionResponse](meq(SubscriptionResponse.key))(any())).thenReturn(Some(SubscriptionResponse("", "", None)))
            when(testCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(None)

            when {
              controller.cacheConnector.save[TradingPremises](any(), meq(TradingPremises.key), any())(any(), any())
            } thenReturn Future.successful(fixedCacheMap)

            when {
              controller.cacheConnector.save[ResponsiblePerson](any(), meq(ResponsiblePerson.key), any())(any(), any())
            } thenReturn Future.successful(fixedCacheMap)

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
          }
        }
      }


      "there is no data in S4L" should {
        "refresh from API5 and redirect to status controller" in new Fixture {
          setUpMocksForNoDataInSaveForLater(controller)

          when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(SubscriptionResponse("", "", None))))

          when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
            .thenReturn(Future.successful(NotCompleted, None))

          val result = controller.get()(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
          verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
        }
      }
    }

    "an enrolment does not exist" when {
      "there is data in S4L " should {
        "not refresh API5 and redirect to status controller" in new FixtureNoAmlsNumber {

          val businessMatching = mock[BusinessMatching]
          val cacheMap = mock[CacheMap]

          when(controller.landingService.cacheMap(any[String])(any(), any())) thenReturn Future.successful(Some(cacheMap))
          when(businessMatching.isComplete) thenReturn true
          when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(businessMatching))
          when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
          when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(None)
          when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any()))
            .thenReturn(Future.successful(NotCompleted, None))

          val result = controller.get()(request)

          verify(controller.landingService, never()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext])
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
        }
      }

      "there is no data in S4L" when {
        "there is data in keystore " should {
          "copy keystore data to S4L and redirect to business type controler" in new FixtureNoAmlsNumber {
            setUpMocksForNoDataInSaveForLater(controller)
            val reviewDetails = setUpMocksForDataExistsInKeystore(controller)

            when(controller.landingService.updateReviewDetails(any[ReviewDetails], any[String])(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(mock[CacheMap]))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessmatching.routes.BusinessTypeController.get().url))

            Mockito.verify(controller.landingService, times(1))
              .updateReviewDetails(any[ReviewDetails], any[String])(any[HeaderCarrier], any[ExecutionContext])
          }
        }

        "there is no data in keystore" should {
          "redirect to business customer" in new FixtureNoAmlsNumber {
            setUpMocksForNoDataInSaveForLater(controller)
            setUpMocksForNoDataInKeyStore(controller)

            val result = controller.get()(request)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://localhost:9923/business-customer/amls"))
          }
        }
      }
    }
  }
}
