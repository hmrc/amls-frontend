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

import connectors.DataCacheConnector
import generators.{AmlsReferenceNumberGenerator, ResponsiblePersonGenerator}
import models.businesscustomer.ReviewDetails
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching, TrustAndCompanyServices}
import models.confirmation.{BreakdownRow, Currency, SubmissionData}
import models.renewal.Renewal
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.tradingpremises.TradingPremises
import models.{AmendVariationRenewalResponse, SubscriptionFees, SubscriptionResponse}
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, OrgAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Principal}
import utils.StatusConstants

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SubmissionResponseServiceSpec extends PlaySpec
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with OneAppPerSuite
  with ResponsiblePersonGenerator
  with generators.tradingpremises.TradingPremisesGenerator
  with AmlsReferenceNumberGenerator {

  trait Fixture {

    val TestSubmissionResponseService = new SubmissionResponseService (
      mock[DataCacheConnector]
    )

    val rpFee: BigDecimal = 100
    val rpFeeWithRate: BigDecimal = 130
    val tpFee: BigDecimal = 115
    val tpFeeWithRate: BigDecimal = 125
    val tpHalfFee: BigDecimal = tpFee / 2
    val tpTotalFee: BigDecimal = tpFee + (tpHalfFee * 3)
    val totalFee: BigDecimal = rpFee + tpTotalFee

    val paymentRefNo = "XA000000000000"

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = amlsRegistrationNumber,
      Some(SubscriptionFees(
        registrationFee = 0,
        fpFee = None,
        fpFeeRate = None,
        premiseFee = 0,
        premiseFeeRate = None,
        totalFees = 0,
        paymentReference = paymentRefNo
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
      paymentReference = Some(paymentRefNo),
      difference = Some(0)
    )

    val variationResponse = AmendVariationRenewalResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 100,
      paymentReference = Some(""),
      difference = Some(0)
    )

    val reviewDetails = mock[ReviewDetails]
    val activities = mock[BusinessActivities]
    val businessMatching = mock[BusinessMatching]
    val cache = mock[CacheMap]
    val principle = Principal(None, Accounts(org = Some(OrgAccount("", Org("TestOrgRef")))))

    when {
      authContext.principal
    } thenReturn principle

    when {
      businessMatching.activities
    } thenReturn Some(activities)

    when {
      activities.businessActivities
    } thenReturn Set[BusinessActivity]()

    when {
      cache.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(businessMatching)

    when {
      cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
    } thenReturn Some(amendmentResponse)

    when {
      cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
    } thenReturn Some(Seq(tradingPremisesGen.sample.get))

    when {
      cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
    } thenReturn Some(Seq(ResponsiblePerson()))

    when {
      TestSubmissionResponseService.cacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))
  }

  "SubmissionResponseService" when {
    "getAmendment is called" must {
      "submit amendment returning submission data" in new Fixture {

        when {
          cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
        } thenReturn Some(Seq(ResponsiblePerson()))

        when {
          TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        val rows = Seq(
          BreakdownRow("confirmation.submission", 1, 100, 100)
        ) ++ Seq(
          BreakdownRow("confirmation.tradingpremises", 1, 115, 0)
        )

        val response = Some(SubmissionData(Some(paymentRefNo), Currency.fromBD(100), rows, None, Some(Currency.fromBD(0))))

        whenReady(TestSubmissionResponseService.getAmendment) {
          result =>
            result must equal(response)
        }
      }

      "submit amendment returning submission data with dynamic fee rate" in new Fixture {

        val amendmentResponseWithRate = AmendVariationRenewalResponse(
          processingDate = "",
          etmpFormBundleNumber = "",
          registrationFee = 100,
          fpFee = Some(500),
          fpFeeRate = Some(250),
          premiseFee = 150,
          premiseFeeRate = Some(150),
          totalFees = 100,
          paymentReference = Some(paymentRefNo),
          difference = Some(0)
        )

        when {
          cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
        } thenReturn Some(amendmentResponseWithRate)

        when {
          cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
        } thenReturn Some(Seq(responsiblePersonGen.sample.get))

        val rows = Seq(
          BreakdownRow("confirmation.submission", 1, 100, 100)
        ) ++ Seq(
          BreakdownRow("confirmation.responsiblepeople", 1, 250, 500)
        ) ++ Seq(
          BreakdownRow("confirmation.tradingpremises", 1, 150, 150)
        )

        val response = Some(SubmissionData(Some(paymentRefNo), Currency.fromBD(100), rows, None, Some(Currency.fromBD(0))))

        whenReady(TestSubmissionResponseService.getAmendment) {
          result =>
            result must equal(response)
        }
      }

      "not show negative fees for responsible people who have already been paid for" in new Fixture {

        val people = Seq(
          ResponsiblePerson(Some(PersonName("Unfit", Some("and"), "Unproper")), hasAlreadyPassedFitAndProper = Some(false)),
          ResponsiblePerson(Some(PersonName("Fit", Some("and"), "Proper")), hasAlreadyPassedFitAndProper = Some(true))
        )

        val amendResponseWithRPFees = amendmentResponse.copy(fpFee = Some(100))

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
        } thenReturn Some(amendResponseWithRPFees)

        when {
          cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
        } thenReturn Some(people)

        val result = await(TestSubmissionResponseService.getAmendment)

        whenReady(TestSubmissionResponseService.getAmendment) {
          _ foreach {
            case SubmissionData(_, _, rows, _, _) =>
              val unpaidRow = rows.filter(_.label == "confirmation.responsiblepeople.fp.passed").head
              unpaidRow.perItm.value mustBe 0
              unpaidRow.total.value mustBe 0
          }
        }

      }

      "not include deleted premises in the amendment confirmation table" in new Fixture {

        val premises = Seq(
          TradingPremises(status = Some(StatusConstants.Deleted)),
          tradingPremisesGen.sample.get
        )

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(premises)

        when {
          cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
        } thenReturn Some(amendmentResponse)

        when(cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())) thenReturn Some(Seq(ResponsiblePerson()))

        val result = await(TestSubmissionResponseService.getAmendment)

        whenReady(TestSubmissionResponseService.getAmendment) { result =>
          result foreach {
            case SubmissionData(_, _, rows, _, _) =>
              rows.filter(_.label == "confirmation.tradingpremises").head.quantity mustBe 1
          }
        }

      }

      "not include responsible people in breakdown" when {

        "they have been deleted" in new Fixture {

          val people = Seq(
            ResponsiblePerson(Some(PersonName("Valid", None, "Person"))),
            ResponsiblePerson(Some(PersonName("Deleted", None, "Person")), status = Some(StatusConstants.Deleted))
          )

          val amendResponseWithRPFees = amendmentResponse.copy(fpFee = Some(100))

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(amendResponseWithRPFees)

          when(cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())) thenReturn Some(people)

          val result = await(TestSubmissionResponseService.getAmendment)

          whenReady(TestSubmissionResponseService.getAmendment)(_ foreach {
            case SubmissionData(_, _, rows, _, _) => rows.filter(_.label == "confirmation.responsiblepeople").head.quantity mustBe 1
          })

        }

        "there is no fee returned in a amendment response because business type is not msb or tcsp" in new Fixture {

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(amendmentResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          val result = await(TestSubmissionResponseService.getAmendment)

          result match {
            case Some(SubmissionData(_, _, rows, _, _)) => rows foreach { row =>
              row.label must not equal "confirmation.responsiblepeople"
              row.label must not equal "confirmation.responsiblepeople.fp.passed"
            }
          }
        }
      }

      "notify user of amendment fees to pay in breakdown" when {

        "the business type is MSB and there is not a Responsible Persons fee to pay from am amendment" in new Fixture {

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(amendmentResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

          val result = await(TestSubmissionResponseService.getAmendment)

          result match {
            case Some(SubmissionData(_, _, rows, _, _)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(0)
            }
          }

        }

        "the business type is TCSP and there is not a Responsible Persons fee to pay from am amendment" in new Fixture {

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(amendmentResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

          val result = await(TestSubmissionResponseService.getAmendment)

          result match {
            case Some(SubmissionData(_, _, rows, _, _)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(0)
            }
          }

        }
      }

      "fall back to getting the subcription response, if the amendment response isn't available" in new Fixture {
        when {
          cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
        } thenReturn None

        when {
          cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
        } thenReturn Some(subscriptionResponse)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
        } thenReturn Some(Seq(ResponsiblePerson()))

        val result = await(TestSubmissionResponseService.getAmendment)

        result mustBe defined

        verify(cache).getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
      }
    }

    "getVariation is called" must {

      "notify user of variation fees to pay in breakdown" when {
        "there is a Responsible People fee to pay" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.fetchAll(any(), any())
          } thenReturn Future.successful(Some(cache))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponse.copy(
            fpFee = Some(100),
            addedResponsiblePeople = 1
          ))

          val result = await(TestSubmissionResponseService.getVariation)

          result match {
            case Some(SubmissionData(_, _, rows, _, _)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(0)
            }
          }
        }
      }


      "not include responsible people in breakdown" when {
        "there is no fee returned in a variation response because business type is not msb or tcsp" in new Fixture {

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponse.copy(fpFee = None))

          val result = await(TestSubmissionResponseService.getVariation)

          result match {
            case Some(SubmissionData(_, _, rows, _, _)) => rows foreach { row =>
              row.label must not equal "confirmation.responsiblepeople"
              row.label must not equal "confirmation.responsiblepeople.fp.passed"
            }
          }
        }
      }

      "notify user of amendment fees to pay in breakdown" when {
        "there is a Responsible Persons fee to pay with dynamic fpFeeRate" in new Fixture {

          val variationResponseWithRate = AmendVariationRenewalResponse(
            processingDate = "",
            etmpFormBundleNumber = "",
            registrationFee = 100,
            fpFee = Some(130.0),
            fpFeeRate = Some(130),
            premiseFee = 0,
            premiseFeeRate = Some(125),
            totalFees = 100,
            paymentReference = Some(""),
            difference = Some(0),
            addedResponsiblePeople = 1
          )

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponseWithRate)

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some(SubmissionData(_, _, breakdownRows, _, _)) =>
              breakdownRows.head.label mustBe "confirmation.responsiblepeople"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(rpFeeWithRate)
              breakdownRows.head.total mustBe Currency(rpFeeWithRate)
              breakdownRows.length mustBe 1

              breakdownRows.count(row => row.label.equals("confirmation.responsiblepeople.fp.passed")) mustBe 0
            case _ => false
          }
        }
      }

      "notify user of variation fees to pay" when {

        "a Trading Premises has been added with a full year fee" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponse.copy(addedFullYearTradingPremises = 1))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some(SubmissionData(_, _, breakdownRows, _, _)) =>
              breakdownRows.head.label mustBe "confirmation.tradingpremises"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(tpFee)
              breakdownRows.head.total mustBe Currency(tpFee)
              breakdownRows.length mustBe 1
            case _ => false
          }
        }

        "a Trading Premises has been added with a half year fee" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponse.copy(halfYearlyTradingPremises = 1))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some(SubmissionData(_, _, breakdownRows, _, _)) =>
              breakdownRows.head.label mustBe "confirmation.tradingpremises.half"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(tpHalfFee)
              breakdownRows.head.total mustBe Currency(tpHalfFee)
              breakdownRows.length mustBe 1
            case _ => false
          }
        }

        "a Trading Premises has been added with a zero fee" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponse.copy(zeroRatedTradingPremises = 1))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some(SubmissionData(_, _, breakdownRows, _, _)) =>
              breakdownRows.head.label mustBe "confirmation.tradingpremises.zero"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(0)
              breakdownRows.head.total mustBe Currency(0)
              breakdownRows.length mustBe 1
            case _ => false
          }
        }

        "the business type is MSB and there is not a Responsible Persons fee to pay from an variation" in new Fixture {

          val testVariationResponse = AmendVariationRenewalResponse(
            processingDate = "",
            etmpFormBundleNumber = "",
            registrationFee = 100,
            fpFee = None,
            fpFeeRate = None,
            premiseFee = 0,
            premiseFeeRate = None,
            totalFees = 100,
            paymentReference = Some(""),
            difference = Some(0),
            addedResponsiblePeopleFitAndProper = 1
          )

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(testVariationResponse)

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

          val result = await(TestSubmissionResponseService.getVariation)

          result match {
            case Some(SubmissionData(_, _, rows, _, _)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(0)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(1)
            }
          }

        }

        "the business type is TCSP and there is not a Responsible Persons fee to pay from an variation" in new Fixture {

          val testVariationResponse = AmendVariationRenewalResponse(
            processingDate = "",
            etmpFormBundleNumber = "",
            registrationFee = 100,
            fpFee = None,
            fpFeeRate = None,
            premiseFee = 0,
            premiseFeeRate = None,
            totalFees = 100,
            paymentReference = Some(""),
            difference = Some(0),
            addedResponsiblePeopleFitAndProper = 1
          )

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(testVariationResponse)

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

          val result = await(TestSubmissionResponseService.getVariation)

          result match {
            case Some(SubmissionData(_, _, rows, _, _)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(0)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(1)
            }
          }

        }

        "each of the categorised fees are in the response" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponse.copy(
            fpFee = Some(rpFee),
            addedResponsiblePeople = 1,
            addedResponsiblePeopleFitAndProper = 1,
            addedFullYearTradingPremises = 1,
            halfYearlyTradingPremises = 1,
            zeroRatedTradingPremises = 1
          ))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some(SubmissionData(_, _, breakdownRows, _, _)) =>
              breakdownRows.head.label mustBe "confirmation.responsiblepeople"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(rpFee)
              breakdownRows.head.total mustBe Currency(rpFee)

              breakdownRows(1).label mustBe "confirmation.responsiblepeople.fp.passed"
              breakdownRows(1).quantity mustBe 1
              breakdownRows(1).perItm mustBe Currency(0)
              breakdownRows(1).total mustBe Currency(0)

              breakdownRows(2).label mustBe "confirmation.tradingpremises.zero"
              breakdownRows(2).quantity mustBe 1
              breakdownRows(2).perItm mustBe Currency(0)
              breakdownRows(2).total mustBe Currency(0)

              breakdownRows(3).label mustBe "confirmation.tradingpremises.half"
              breakdownRows(3).quantity mustBe 1
              breakdownRows(3).perItm mustBe Currency(tpHalfFee)
              breakdownRows(3).total mustBe Currency(tpHalfFee)

              breakdownRows.last.label mustBe "confirmation.tradingpremises"
              breakdownRows.last.quantity mustBe 1
              breakdownRows.last.perItm mustBe Currency(tpFee)
              breakdownRows.last.total mustBe Currency(tpFee)

            case _ => false
          }
        }
      }
    }

    "getSubscription is called" must {
      "notify user of subscription fees to pay in breakdown" when {

        "there is a Responsible People fee to pay" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.fetchAll(any(), any())
          } thenReturn Future.successful(Some(cache))

          when {
            cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
          } thenReturn Some(subscriptionResponse.copy(subscriptionFees=Some(subscriptionResponse.subscriptionFees.get.copy(fpFee = Some(100)))))

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case SubmissionData(_, _, rows, _, _) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(0)
            }
          }
        }

        "there is a Responsible People fee to pay and fpRate should be read dynamically" in new Fixture {

          val subscriptionResponseWithFeeRate = SubscriptionResponse(
            etmpFormBundleNumber = "",
            amlsRefNo = amlsRegistrationNumber,
            Some(SubscriptionFees(
            registrationFee = 100,
            fpFee = Some(125.0),
            fpFeeRate = Some(130.0),
            premiseFee = 0,
            premiseFeeRate = Some(125.0),
            totalFees = 0,
            paymentReference = ""
            ))
          )

          when {
            cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
          } thenReturn Some(subscriptionResponseWithFeeRate)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(responsiblePersonGen.sample.get, responsiblePersonGen.sample.get))

          val result = await(TestSubmissionResponseService.getSubscription)

          case class Test(str: String)

          result match {
            case SubmissionData(_, _, rows, _, _) => {
              rows.head.label mustBe "confirmation.submission"
              rows.head.quantity mustBe 1
              rows.head.perItm mustBe Currency(rpFee)
              rows.head.total mustBe Currency(rpFee)

              rows(1).label mustBe "confirmation.responsiblepeople"
              rows(1).quantity mustBe 2
              rows(1).perItm mustBe Currency(rpFeeWithRate)
              rows(1).total mustBe Currency(125.0)

              rows.last.label mustBe "confirmation.tradingpremises"
              rows.last.quantity mustBe 1
              rows.last.perItm mustBe Currency(tpFeeWithRate)
              rows.last.total mustBe Currency(0)
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(0)
            }
          }
        }

        "the business type is MSB and there is not a Responsible Persons fee to pay from a subscription" in new Fixture {

          when {
            cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
          } thenReturn Some(subscriptionResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case SubmissionData(_, _, rows, _, _) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(0)
            }
          }
        }

        "the business type is TCSP and there is not a Responsible Persons fee to pay from a subscription" in new Fixture {

          when {
            cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
          } thenReturn Some(subscriptionResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case SubmissionData(_, _, rows, _, _) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.responsiblepeople.fp.passed")) must be(0)
            }
          }
        }
      }

      "not include responsible people in breakdown" when {

        "there is no fee returned in a subscription response because business type is not msb or tcsp" in new Fixture {

          when {
            cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
          } thenReturn Some(subscriptionResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case SubmissionData(_, _, rows, _, _) => rows foreach { row =>
              row.label must not equal "confirmation.responsiblepeople"
              row.label must not equal "confirmation.responsiblepeople.fp.passed"
            }
          }
        }
      }
    }

    "getSubmissionData is called" must {

      "return submission data" when {

        "Submission/SubmissionReady" in new Fixture {

          val currency = Currency.fromInt(0)

          val submissionData = SubmissionData(Some(paymentRefNo), currency, Seq(
            BreakdownRow("confirmation.submission", 0, 0, 0),
            BreakdownRow("confirmation.tradingpremises", 1, 115, 0)
          ), Some(amlsRegistrationNumber), None)

          when {
            cache.getEntry[SubscriptionResponse](SubscriptionResponse.key)
          } thenReturn Some(subscriptionResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          val result = TestSubmissionResponseService.getSubmissionData(SubmissionReady)

          await(result) mustBe Some(submissionData)

        }

        "Amendment/SubmissionReadyForReview" in new Fixture {

          val currency = Currency.fromInt(100)

          val submissionData = SubmissionData(Some(paymentRefNo), currency, Seq(
            BreakdownRow("confirmation.submission", 1, 100, 100),
            BreakdownRow("confirmation.tradingpremises", 1, 115, 0)
          ), None, Some(currency))

          when {
            cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
          } thenReturn Some(amendmentResponse.copy(difference = Some(100)))

          when {
            cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(ResponsiblePerson()))

          val result = TestSubmissionResponseService.getSubmissionData(SubmissionReadyForReview)

          await(result) mustBe Some(submissionData)

        }

        "Variation/SubmissionDecisionApproved" in new Fixture {

          val currency = Currency.fromInt(100)

          val submissionData = SubmissionData(Some(paymentRefNo), currency, Seq(), None, Some(currency))

          when {
            cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
          } thenReturn Some(amendmentResponse.copy(difference = Some(100)))

          val result = TestSubmissionResponseService.getSubmissionData(SubmissionDecisionApproved)

          await(result) mustBe Some(submissionData)

        }

        "Renewal/ReadyForRenewal" in new Fixture {

          val currency = Currency.fromInt(100)

          val submissionData = SubmissionData(Some(paymentRefNo), currency, Seq(), None, Some(currency))

          when {
            cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
          } thenReturn Some(amendmentResponse.copy(difference = Some(100)))

          when {
            TestSubmissionResponseService.cacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(),any(),any())
          } thenReturn Future.successful(Some(Renewal()))

          val result = TestSubmissionResponseService.getSubmissionData(ReadyForRenewal(Some(LocalDate.now())))

          await(result) mustBe Some(submissionData)

        }
      }

    }

  }
}
