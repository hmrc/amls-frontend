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

import config.ApplicationConfig
import connectors.AmlsConnector
import exceptions.{DuplicateSubscriptionException, NoEnrolmentException}
import generators.ResponsiblePersonGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models._
import models.amp.Amp
import models.bankdetails.BankDetails
import models.businessactivities.{BusinessActivities => BusActivities}
import models.businesscustomer.ReviewDetails
import models.businessdetails.{BusinessDetails, RegisteredOfficeUK}
import models.businessmatching.BusinessType.SoleProprietor
import models.businessmatching._
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal._
import models.responsiblepeople.ResponsiblePerson
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import utils.{AmlsSpec, DependencyMocks}

import java.time.LocalDate
import scala.concurrent.Future

class SubmissionServiceSpec extends AmlsSpec
  with ScalaFutures
  with IntegrationPatience
  with ResponsiblePersonGenerator
  with TradingPremisesGenerator {

  override lazy val app = GuiceApplicationBuilder()
    .configure(
      "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
    )
    .build()

  trait Fixture extends DependencyMocks {

    val config = mock[ApplicationConfig]

    val submissionService = new SubmissionService (
      mockCacheConnector,
      mock[AuthEnrolmentsService],
      mock[AmlsConnector],
      config,
      mock[connectors.BusinessMatchingConnector]
    )

    val enrolmentResponse = HttpResponse(OK, "")

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "amlsRef",
      Some(SubscriptionFees(
        registrationFee = 0,
        fpFee = None,
        fpFeeRate = None,
        approvalCheckFee = None,
        approvalCheckFeeRate = None,
        premiseFee = 0,
        premiseFeeRate = None,
        totalFees = 0,
        paymentReference = ""
      )))

    val amendmentResponse = AmendVariationRenewalResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = None,
      approvalCheckFee = None,
      approvalCheckFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 100,
      paymentReference = Some("XA000000000000"),
      difference = Some(0)
    )

    val safeId = "safeId"
    val amlsRegistrationNumber = "amlsRegNo"
    val businessType = SoleProprietor

    val reviewDetails = mock[ReviewDetails]
    val activities = mock[BusinessActivities]
    val businessMatching = mock[BusinessMatching]
    val businessDetails = mock[BusinessDetails]
    val amp = Amp(Json.obj(
      "typeOfParticipant"      -> Seq("artGalleryOwner"),
      "soldOverThreshold"             -> true,
      "dateTransactionOverThreshold"  -> LocalDate.now.toString,
      "identifyLinkedTransactions"    -> true,
      "percentageExpectedTurnover"    -> "zeroToTwenty"
    ))

    mockCacheFetchAll
    mockCacheSave[SubscriptionResponse]
    mockCacheSave[AmendVariationRenewalResponse]
    mockCacheSave[SubmissionRequestStatus]

    when {
      reviewDetails.safeId
    } thenReturn safeId
    when {
      reviewDetails.businessType
    } thenReturn Some(businessType)
    when {
      businessMatching.reviewDetails
    } thenReturn Some(reviewDetails)
    when {
      businessDetails.registeredOffice
    } thenReturn Some(RegisteredOfficeUK("Line 1", None, None, None, "postcode", None))
    when {
      businessMatching.activities
    } thenReturn Some(activities)
    when {
      activities.businessActivities
    } thenReturn Set[BusinessActivity]()

    mockCacheGetEntry[BusActivities](Some(BusActivities()), BusActivities.key)
    mockCacheGetEntry[Amp](Some(amp), Amp.key)
    mockCacheGetEntry[MoneyServiceBusiness](Some(MoneyServiceBusiness()), MoneyServiceBusiness.key)
    mockCacheGetEntry[Hvd](Some(Hvd()), Hvd.key)
    mockCacheGetEntry[BusinessMatching](Some(businessMatching), BusinessMatching.key)
    mockCacheGetEntry[Eab](Some(mock[Eab]), Eab.key)
    mockCacheGetEntry[BusinessDetails](Some(businessDetails), BusinessDetails.key)
    mockCacheGetEntry[Seq[BankDetails]](Some(Seq(BankDetails())), BankDetails.key)
    mockCacheGetEntry[Seq[ResponsiblePerson]](Some(Seq(responsiblePersonGen.sample.get)), ResponsiblePerson.key)
    mockCacheGetEntry[AmendVariationRenewalResponse](Some(amendmentResponse), AmendVariationRenewalResponse.key)
    mockCacheGetEntry[Seq[TradingPremises]](Gen.listOf(tradingPremisesGen).sample, TradingPremises.key)

  }

  "SubmissionService" when {

    "subscribe is called" must {
      "subscribe and enrol using Government Gateway" when {
        "the enrolment-store toggle is off" in new Fixture {

          when(config.enrolmentStoreToggle) thenReturn false

          when {
            submissionService.amlsConnector.subscribe(any(), eqTo(safeId), any())(any(), any(), any(), any())
          } thenReturn Future.successful(subscriptionResponse)

        }
      }

      "subscribe and enrol using Enrolment Store" when {
        "the enrolment-store toggle is on" in new Fixture {
          when(config.enrolmentStoreToggle) thenReturn true

          when {
            submissionService.amlsConnector.subscribe(any(), eqTo(safeId), any())(any(), any(), any(), any())
          } thenReturn Future.successful(subscriptionResponse)

          when {
            submissionService.authEnrolmentsService.enrol(any(), any(), any(), any())(any(), any())
          } thenReturn Future.successful(HttpResponse(OK, ""))

          whenReady(submissionService.subscribe("12345678", ("accType", "id"), Some("GROUP_ID"))) {
            _ mustBe subscriptionResponse
          }
        }
      }

      "throw a DuplicateSubscriptionException when a 422 is returned from AMLS" in new Fixture {
        when(config.enrolmentStoreToggle) thenReturn true

        when {
          submissionService.amlsConnector.subscribe(any(), eqTo(safeId), any())(any(), any(), any(), any())
        } thenReturn Future.failed(
          UpstreamErrorResponse(Json.toJson(SubscriptionErrorResponse(amlsRegistrationNumber, "An error occurred")).toString(),
            UNPROCESSABLE_ENTITY,
            UNPROCESSABLE_ENTITY))

        intercept[DuplicateSubscriptionException] {
          await(submissionService.subscribe("12345678", ("accType", "id"), Some("GROUP_ID")))
        }

        verify(submissionService.authEnrolmentsService, never).enrol(any(), any(), any(), any())(any(), any())
      }

      "throw the original error if 422 encountered without a json body" in new Fixture {
        when(config.enrolmentStoreToggle) thenReturn true

        when {
          submissionService.amlsConnector.subscribe(any(), eqTo(safeId), any())(any(), any(), any(), any())
        } thenReturn Future.failed(UpstreamErrorResponse("Some other kind of error occurred", UNPROCESSABLE_ENTITY, UNPROCESSABLE_ENTITY))

        intercept[UpstreamErrorResponse] {
          await(submissionService.subscribe("12345678", ("accType", "id"), Some("GROUP_ID")))
        }

        verify(submissionService.authEnrolmentsService, never).enrol(any(), any(), any(), any())(any(), any())
      }

      "throw the original error if something other than a duplicate subscription is encountered" in new Fixture {
        when(config.enrolmentStoreToggle) thenReturn true

        when {
          submissionService.amlsConnector.subscribe(any(), eqTo(safeId), any())(any(), any(), any(), any())
        } thenReturn Future.failed(UpstreamErrorResponse("Some other kind of error occurred", BAD_REQUEST, BAD_REQUEST))

        intercept[UpstreamErrorResponse] {
          await(submissionService.subscribe("12345678", ("accType", "id"), Some("GROUP_ID")))
        }

        verify(submissionService.authEnrolmentsService, never).enrol(any(), any(), any(), any())(any(), any())
      }

    }

    "update is called" must {
      "submit amendment" in new Fixture {

        when {
          submissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber), any())(any(), any(), any(), any())
        } thenReturn Future.successful(amendmentResponse)

        whenReady(submissionService.update("12345678", Some(amlsRegistrationNumber), ("accType", "id"))) {
          result =>
            result must equal(amendmentResponse)
        }
      }

      "return failed future when no enrolment" in new Fixture {

        whenReady(submissionService.update("12345678", None, ("accType", "id")).failed) {
          result =>
            result mustBe a[NoEnrolmentException]
        }
      }
    }

    "variation is called" must {
      "submit variation" in new Fixture {

        when {
          submissionService.amlsConnector.variation(any(), eqTo(amlsRegistrationNumber), any())(any(), any(), any(), any())
        } thenReturn Future.successful(amendmentResponse)

        whenReady(submissionService.variation("12345678", Some(amlsRegistrationNumber), ("accType", "id"))) {
          result =>
            result must equal(amendmentResponse)
        }
      }
    }

    "submit a renewal" in new Fixture {

      when {
        submissionService.amlsConnector.renewal(any(), eqTo(amlsRegistrationNumber), any())(any(), any())
      } thenReturn Future.successful(mock[AmendVariationRenewalResponse])

      val renewal = Renewal(
        turnover = Some(AMLSTurnover.First),
        businessTurnover = Some(BusinessTurnover.Second),
        ampTurnover = Some(AMPTurnover.Second),
        totalThroughput = Some(TotalThroughput("02")),
        customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("Test", "T"))))),
        involvedInOtherActivities = Some(InvolvedInOtherNo),
        mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(
          Country("United Kingdom", "GB"), Country("France", "FR"), Country("us", "US")))),
        whichCurrencies = Some(WhichCurrencies(
          Seq("USD", "CHF", "EUR"), Some(UsesForeignCurrenciesYes), Some(MoneySources(Some(models.renewal.BankMoneySource("Bank Names")), Some(models.renewal.WholesalerMoneySource("wholesaler")), Some(true))))),
        ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("12345678963")),
        transactionsInLast12Months = Some(TransactionsInLast12Months("2500")),
        percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
        receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true,true,Some("other")))))),
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
        fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("10"))
      )

      await(submissionService.renewal("12345678", Some(amlsRegistrationNumber), ("accType", "id"), renewal))

      val captor = ArgumentCaptor.forClass(classOf[SubscriptionRequest])
      verify(submissionService.amlsConnector).renewal(captor.capture(), any(), any())(any(), any())

      val submission = captor.getValue

      // The actual values of these are tested in renewals.models.Conversions
      submission.businessActivitiesSection mustBe defined
      submission.businessActivitiesSection.get.expectedAMLSTurnover mustBe defined
      submission.businessActivitiesSection.get.expectedBusinessTurnover mustBe defined
      submission.businessActivitiesSection.get.customersOutsideUK mustBe defined
      submission.businessActivitiesSection.get.involvedInOther mustBe defined

      submission.msbSection mustBe defined
      submission.msbSection.get.throughput mustBe defined
      submission.msbSection.get.sendMoneyToOtherCountry mustBe defined
      submission.msbSection.get.mostTransactions mustBe defined
      submission.msbSection.get.sendTheLargestAmountsOfMoney mustBe defined
      submission.msbSection.get.fxTransactionsInNext12Months mustBe defined
      submission.msbSection.get.whichCurrencies mustBe defined
      submission.msbSection.get.ceTransactionsInNext12Months mustBe defined
      submission.msbSection.get.transactionsInNext12Months mustBe defined

      submission.hvdSection.get.percentageOfCashPaymentOver15000 mustBe defined
      submission.hvdSection.get.receiveCashPayments mustBe defined
    }

    "submit a renewal amendment" in new Fixture {

      when {
        submissionService.amlsConnector.renewalAmendment(any(), eqTo(amlsRegistrationNumber), any())(any(), any())
      } thenReturn Future.successful(mock[AmendVariationRenewalResponse])

      val renewal = Renewal(
        turnover = Some(AMLSTurnover.First),
        businessTurnover = Some(BusinessTurnover.Second),
        ampTurnover = Some(AMPTurnover.Second),
        totalThroughput = Some(TotalThroughput("02")),
        customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("Test", "T"))))),
        involvedInOtherActivities = Some(InvolvedInOtherNo),
        mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(
          Country("United Kingdom", "GB"), Country("France", "FR"), Country("us", "US")))),
        whichCurrencies = Some(WhichCurrencies(
          Seq("USD", "CHF", "EUR"), Some(UsesForeignCurrenciesYes), Some(MoneySources(Some(models.renewal.BankMoneySource("Bank Names")), Some(models.renewal.WholesalerMoneySource("Wholesaler")), Some(true))))),
        ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("12345678963")),
        transactionsInLast12Months = Some(TransactionsInLast12Months("2500")),
        percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
        receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true,true,Some("other"))))))
      )

      await(submissionService.renewalAmendment("12345678", Some(amlsRegistrationNumber), ("accType", "id"), renewal))

      val captor = ArgumentCaptor.forClass(classOf[SubscriptionRequest])
      verify(submissionService.amlsConnector).renewalAmendment(captor.capture(), any(), any())(any(), any())

      val submission = captor.getValue
      // The actual values of these are tested in renewals.models.Conversions
      submission.businessActivitiesSection mustBe defined
      submission.businessActivitiesSection.get.expectedAMLSTurnover mustBe defined
      submission.businessActivitiesSection.get.expectedBusinessTurnover mustBe defined
      submission.businessActivitiesSection.get.customersOutsideUK mustBe defined
      submission.businessActivitiesSection.get.involvedInOther mustBe defined

      submission.msbSection mustBe defined
      submission.msbSection.get.throughput mustBe defined
      submission.msbSection.get.mostTransactions mustBe defined
      submission.msbSection.get.sendTheLargestAmountsOfMoney mustBe defined
      submission.msbSection.get.whichCurrencies mustBe defined
      submission.msbSection.get.ceTransactionsInNext12Months mustBe defined
      submission.msbSection.get.transactionsInNext12Months mustBe defined

      submission.hvdSection.get.percentageOfCashPaymentOver15000 mustBe defined
      submission.hvdSection.get.receiveCashPayments mustBe defined

    }

  }
}
