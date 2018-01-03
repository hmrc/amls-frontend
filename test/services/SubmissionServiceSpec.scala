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

import connectors.AmlsConnector
import exceptions.NoEnrolmentException
import generators.ResponsiblePersonGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models._
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOfficeUK}
import models.bankdetails.BankDetails
import models.businessactivities.{BusinessActivities => BusActivities}
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessType.SoleProprietor
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.{BankMoneySource, MoneyServiceBusiness}
import models.renewal._
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.Principal
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, OrgAccount}
import utils.DependencyMocks
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Principal}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class SubmissionServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with OneAppPerSuite
  with ResponsiblePersonGenerator
  with TradingPremisesGenerator {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.amounts.registration" -> 100))

  trait Fixture extends DependencyMocks {

    val TestSubmissionService = new SubmissionService {
      override private[services] val cacheConnector = mockCacheConnector
      override private[services] val amlsConnector = mock[AmlsConnector]
      override private[services] val ggService = mock[GovernmentGatewayService]
      override private[services] val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    when {
      mockAuthContext.principal
    } thenReturn Principal(None, Accounts(org = Some(OrgAccount("", Org("TestOrgRef")))))

    val enrolmentResponse = HttpResponse(OK)

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "amlsRef",Some(SubscriptionFees(
        registrationFee = 0,
      fpFee = None,
      fpFeeRate = None,
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
    val aboutTheBusiness = mock[AboutTheBusiness]

    mockCacheFetchAll
    mockCacheSave[SubscriptionResponse]
    mockCacheSave[AmendVariationRenewalResponse]

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
      aboutTheBusiness.registeredOffice
    } thenReturn Some(RegisteredOfficeUK("Line 1", "Line 2", None, None, "postcode", None))
    when {
      businessMatching.activities
    } thenReturn Some(activities)
    when {
      activities.businessActivities
    } thenReturn Set[BusinessActivity]()

    mockCacheGetEntry[BusActivities](Some(BusActivities()), BusActivities.key)
    mockCacheGetEntry[MoneyServiceBusiness](Some(MoneyServiceBusiness()), MoneyServiceBusiness.key)
    mockCacheGetEntry[Hvd](Some(Hvd()), Hvd.key)
    mockCacheGetEntry[BusinessMatching](Some(businessMatching), BusinessMatching.key)
    mockCacheGetEntry[EstateAgentBusiness](Some(mock[EstateAgentBusiness]), EstateAgentBusiness.key)
    mockCacheGetEntry[AboutTheBusiness](Some(aboutTheBusiness), AboutTheBusiness.key)
    mockCacheGetEntry[Seq[BankDetails]](Some(Seq(BankDetails())), BankDetails.key)
    mockCacheGetEntry[Seq[ResponsiblePeople]](Some(Seq(responsiblePersonGen.sample.get)), ResponsiblePeople.key)
    mockCacheGetEntry[AmendVariationRenewalResponse](Some(amendmentResponse), AmendVariationRenewalResponse.key)
    mockCacheGetEntry[Seq[TradingPremises]](Gen.listOf(tradingPremisesGen).sample, TradingPremises.key)

  }

  "SubmissionService" when {

    "subscribe is called" must {
      "subscribe and enrol" in new Fixture {

        when {
          TestSubmissionService.amlsConnector.subscribe(any(), eqTo(safeId))(any(), any(), any(), any(), any())
        } thenReturn Future.successful(subscriptionResponse)

        when {
          TestSubmissionService.ggService.enrol(eqTo("amlsRef"), eqTo(safeId), eqTo("postcode"))(any(), any())
        } thenReturn Future.successful(enrolmentResponse)

        whenReady(TestSubmissionService.subscribe) {
          result =>
            result must equal(subscriptionResponse)
        }
      }
    }

    "update is called" must {
      "submit amendment" in new Fixture {

        when {
          TestSubmissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
        } thenReturn Future.successful(amendmentResponse)

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        }.thenReturn(Future.successful(Some(amlsRegistrationNumber)))

        whenReady(TestSubmissionService.update) {
          result =>
            result must equal(amendmentResponse)
        }
      }

      "return failed future when no enrolment" in new Fixture {

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        }.thenReturn(Future.successful(None))

        whenReady(TestSubmissionService.update.failed) {
          result =>
            result mustBe a[NoEnrolmentException]
        }
      }
    }

    "variation is called" must {
      "submit variation" in new Fixture {

        when {
          TestSubmissionService.amlsConnector.variation(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
        } thenReturn Future.successful(amendmentResponse)

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        }.thenReturn(Future.successful(Some(amlsRegistrationNumber)))

        whenReady(TestSubmissionService.variation) {
          result =>
            result must equal(amendmentResponse)
        }
      }
    }

    "submit a renewal" in new Fixture {

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some(amlsRegistrationNumber))

      when {
        TestSubmissionService.amlsConnector.renewal(any(), eqTo(amlsRegistrationNumber))(any(), any(), any())
      } thenReturn Future.successful(mock[AmendVariationRenewalResponse])

      val renewal = Renewal(
        turnover = Some(AMLSTurnover.First),
        businessTurnover = Some(BusinessTurnover.Second),
        totalThroughput = Some(TotalThroughput("02")),
        customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("Test", "T"))))),
        involvedInOtherActivities = Some(InvolvedInOtherNo),
        mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(
          Country("United Kingdom", "GB"), Some(Country("France", "FR")), Some(Country("us", "US")))),
        whichCurrencies = Some(WhichCurrencies(
          Seq("USD", "CHF", "EUR"), None, Some(BankMoneySource("Bank names")), None, None)),
        ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("12345678963")),
        transactionsInLast12Months = Some(TransactionsInLast12Months("2500")),
        percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
        receiveCashPayments = Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other")))))
      )

      val result = await(TestSubmissionService.renewal(renewal))

      val captor = ArgumentCaptor.forClass(classOf[SubscriptionRequest])
      verify(TestSubmissionService.amlsConnector).renewal(captor.capture(), any())(any(), any(), any())

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

    "submit a renewal amendment" in new Fixture {

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some(amlsRegistrationNumber))

      when {
        TestSubmissionService.amlsConnector.renewalAmendment(any(), eqTo(amlsRegistrationNumber))(any(), any(), any())
      } thenReturn Future.successful(mock[AmendVariationRenewalResponse])

      val renewal = Renewal(
        turnover = Some(AMLSTurnover.First),
        businessTurnover = Some(BusinessTurnover.Second),
        totalThroughput = Some(TotalThroughput("02")),
        customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("Test", "T"))))),
        involvedInOtherActivities = Some(InvolvedInOtherNo),
        mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(
          Country("United Kingdom", "GB"), Some(Country("France", "FR")), Some(Country("us", "US")))),
        whichCurrencies = Some(WhichCurrencies(
          Seq("USD", "CHF", "EUR"), None, Some(BankMoneySource("Bank names")), None, None)),
        ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("12345678963")),
        transactionsInLast12Months = Some(TransactionsInLast12Months("2500")),
        percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
        receiveCashPayments = Some(ReceiveCashPayments(Some(PaymentMethods(true,true,Some("other")))))
      )

      val result = await(TestSubmissionService.renewalAmendment(renewal))

      val captor = ArgumentCaptor.forClass(classOf[SubscriptionRequest])
      verify(TestSubmissionService.amlsConnector).renewalAmendment(captor.capture(), any())(any(), any(), any())

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
