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

import connectors.DataCacheConnector
import models.businesscustomer.ReviewDetails
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching, TrustAndCompanyServices}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import models.tradingpremises.TradingPremises
import models.{AmendVariationRenewalResponse, SubscriptionFees, SubscriptionResponse}
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
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.StatusConstants

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future


class SubmissionResponseServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with OneAppPerSuite {

  trait Fixture {

    val TestSubmissionResponseService = new SubmissionResponseService {
      override private[services] val cacheConnector = mock[DataCacheConnector]
    }

    val rpFee: BigDecimal = 100
    val rpFeeWithRate: BigDecimal = 130
    val tpFee: BigDecimal = 115
    val tpFeeWithRate: BigDecimal = 125
    val tpHalfFee: BigDecimal = tpFee / 2
    val tpTotalFee: BigDecimal = tpFee + (tpHalfFee * 3)
    val totalFee: BigDecimal = rpFee + tpTotalFee

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "amlsRef", Some(SubscriptionFees(
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
      difference = Some(0),
      addedResponsiblePeople = 0,
      addedFullYearTradingPremises = 0,
      halfYearlyTradingPremises = 0,
      zeroRatedTradingPremises = 0
    )

    val amlsRegistrationNumber = "amlsRegNo"
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
      TestSubmissionResponseService.cacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))
  }

  "SubmissionResponseService" when {
    "getAmendment is called" must {
      "submit amendment returning submission data" in new Fixture {

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        when {
          TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        val rows = Seq(
          BreakdownRow("confirmation.submission", 1, 100, 100)
        ) ++ Seq(
          BreakdownRow("confirmation.tradingpremises", 1, 115, 0)
        )

        val response = Some(Some("XA000000000000"), Currency.fromBD(100), rows, Some(Currency.fromBD(0)))

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
          paymentReference = Some("XA000000000000"),
          difference = Some(0)
        )

        when {
          cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
        } thenReturn Some(amendmentResponseWithRate)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        val rows = Seq(
          BreakdownRow("confirmation.submission", 1, 100, 100)
        ) ++ Seq(
          BreakdownRow("confirmation.responsiblepeople", 1, 250, 500)
        ) ++ Seq(
          BreakdownRow("confirmation.tradingpremises", 1, 150, 150)
        )

        val response = Some(Some("XA000000000000"), Currency.fromBD(100), rows, Some(Currency.fromBD(0)))

        whenReady(TestSubmissionResponseService.getAmendment) {
          result =>
            result must equal(response)
        }
      }

      "not show negative fees for responsible people who have already been paid for" in new Fixture {

        val people = Seq(
          ResponsiblePeople(Some(PersonName("Unfit", Some("and"), "Unproper", None, None)), hasAlreadyPassedFitAndProper = Some(false)),
          ResponsiblePeople(Some(PersonName("Fit", Some("and"), "Proper", None, None)), hasAlreadyPassedFitAndProper = Some(true))
        )

        val amendResponseWithRPFees = amendmentResponse.copy(fpFee = Some(100))

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
        } thenReturn Some(amendResponseWithRPFees)

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(people)

        val result = await(TestSubmissionResponseService.getAmendment)

        whenReady(TestSubmissionResponseService.getAmendment) {
          _ foreach {
            case (_, _, rows, _) =>
              val unpaidRow = rows.filter(_.label == "confirmation.unpaidpeople").head
              unpaidRow.perItm.value mustBe 0
              unpaidRow.total.value mustBe 0
          }
        }

      }

      "not include deleted premises in the amendment confirmation table" in new Fixture {

        val premises = Seq(
          TradingPremises(status = Some(StatusConstants.Deleted)),
          TradingPremises()
        )

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(premises)

        when {
          cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
        } thenReturn Some(amendmentResponse)

        when(cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())) thenReturn Some(Seq(ResponsiblePeople()))

        val result = await(TestSubmissionResponseService.getAmendment)

        whenReady(TestSubmissionResponseService.getAmendment) { result =>
          result foreach {
            case (_, _, rows, _) =>
              rows.filter(_.label == "confirmation.tradingpremises").head.quantity mustBe 1
          }
        }

      }

      "not include responsible people in breakdown" when {

        "they have been deleted" in new Fixture {

          val people = Seq(
            ResponsiblePeople(Some(PersonName("Valid", None, "Person", None, None))),
            ResponsiblePeople(Some(PersonName("Deleted", None, "Person", None, None)), status = Some(StatusConstants.Deleted))
          )

          val amendResponseWithRPFees = amendmentResponse.copy(fpFee = Some(100))

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(amendResponseWithRPFees)

          when(cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())) thenReturn Some(people)

          val result = await(TestSubmissionResponseService.getAmendment)

          whenReady(TestSubmissionResponseService.getAmendment)(_ foreach {
            case (_, _, rows, _) => rows.filter(_.label == "confirmation.responsiblepeople").head.quantity mustBe 1
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          val result = await(TestSubmissionResponseService.getAmendment)

          result match {
            case Some((_, _, rows, _)) => rows foreach { row =>
              row.label must not equal "confirmation.responsiblepeople"
              row.label must not equal "confirmation.unpaidpeople"
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

          val result = await(TestSubmissionResponseService.getAmendment)

          result match {
            case Some((_, _, rows, _)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

          val result = await(TestSubmissionResponseService.getAmendment)

          result match {
            case Some((_, _, rows, _)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
            }
          }

        }
      }
    }

    "getVariation is called" must {
      "retrieve data from variation submission" in new Fixture {

        when {
          TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        when {
          cache.getEntry[AmendVariationRenewalResponse](any())(any())
        } thenReturn Some(variationResponse.copy(
          paymentReference = Some("12345"),
          addedResponsiblePeople = 1,
          addedFullYearTradingPremises = 1,
          halfYearlyTradingPremises = 3,
          zeroRatedTradingPremises = 1
        ))

        val rows = Seq(
          BreakdownRow("confirmation.responsiblepeople", 1, Currency(100), Currency(rpFee))
        ) ++ Seq(
          BreakdownRow("confirmation.tradingpremises.zero", 1, Currency(0), Currency(0))
        ) ++ Seq(
          BreakdownRow("confirmation.tradingpremises.half", 3, Currency(57.50), Currency(tpHalfFee * 3))
        ) ++ Seq(
          BreakdownRow("confirmation.tradingpremises", 1, Currency(115), Currency(tpFee))
        )

        val response = Some(Some("12345"), Currency.fromBD(totalFee), rows)

        whenReady(TestSubmissionResponseService.getVariation) {
          result =>
            result must equal(response)
        }

      }

      "not include responsible people in breakdown" when {

        "there is no fee returned in a variation response because business type is not msb or tcsp" in new Fixture {

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(variationResponse.copy(fpFee = None))

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          val result = await(TestSubmissionResponseService.getVariation)

          result match {
            case Some((_, _, rows)) => rows foreach { row =>
              row.label must not equal "confirmation.responsiblepeople"
              row.label must not equal "confirmation.unpaidpeople"
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
            fpFee = None,
            fpFeeRate = Some(130),
            premiseFee = 0,
            premiseFeeRate = Some(125),
            totalFees = 100,
            paymentReference = Some(""),
            difference = Some(0),
            addedResponsiblePeople = 1,
            addedFullYearTradingPremises = 0,
            halfYearlyTradingPremises = 0,
            zeroRatedTradingPremises = 0
          )

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](any())(any())
          } thenReturn Some(variationResponseWithRate)

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some((_, _, breakdownRows)) =>
              breakdownRows.head.label mustBe "confirmation.responsiblepeople"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(rpFeeWithRate)
              breakdownRows.head.total mustBe Currency(rpFeeWithRate)
              breakdownRows.length mustBe 1

              breakdownRows.count(row => row.label.equals("confirmation.unpaidpeople")) mustBe 0
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
            cache.getEntry[AmendVariationRenewalResponse](any())(any())
          } thenReturn Some(variationResponse.copy(addedFullYearTradingPremises = 1))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some((_, _, breakdownRows)) =>
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
            cache.getEntry[AmendVariationRenewalResponse](any())(any())
          } thenReturn Some(variationResponse.copy(halfYearlyTradingPremises = 1))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some((_, _, breakdownRows)) =>
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
            cache.getEntry[AmendVariationRenewalResponse](any())(any())
          } thenReturn Some(variationResponse.copy(zeroRatedTradingPremises = 1))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some((_, _, breakdownRows)) =>
              breakdownRows.head.label mustBe "confirmation.tradingpremises.zero"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(0)
              breakdownRows.head.total mustBe Currency(0)
              breakdownRows.length mustBe 1
            case _ => false
          }
        }

        "there is a Responsible Persons fee to pay" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](any())(any())
          } thenReturn Some(variationResponse.copy(addedResponsiblePeople = 1))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some((_, _, breakdownRows)) =>
              breakdownRows.head.label mustBe "confirmation.responsiblepeople"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(rpFee)
              breakdownRows.head.total mustBe Currency(rpFee)
              breakdownRows.length mustBe 1

              breakdownRows.count(row => row.label.equals("confirmation.unpaidpeople")) mustBe 0
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
            addedResponsiblePeople = 0,
            addedFullYearTradingPremises = 0,
            halfYearlyTradingPremises = 0,
            zeroRatedTradingPremises = 0,
            addedResponsiblePeopleFitAndProper = 1
          )

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(testVariationResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

          val result = await(TestSubmissionResponseService.getVariation)

          result match {
            case Some((_, _, rows)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(0)
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(1)
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
            addedResponsiblePeople = 0,
            addedFullYearTradingPremises = 0,
            halfYearlyTradingPremises = 0,
            zeroRatedTradingPremises = 0,
            addedResponsiblePeopleFitAndProper = 1
          )

          when {
            cache.getEntry[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key))(any())
          } thenReturn Some(testVariationResponse)

          when {
            cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
          } thenReturn Some(Seq(TradingPremises()))

          when {
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

          val result = await(TestSubmissionResponseService.getVariation)

          result match {
            case Some((_, _, rows)) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(0)
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(1)
            }
          }

        }

        "each of the categorised fees are in the response" in new Fixture {

          when {
            TestSubmissionResponseService.cacheConnector.save[AmendVariationRenewalResponse](eqTo(AmendVariationRenewalResponse.key), any())(any(), any(), any())
          } thenReturn Future.successful(CacheMap("", Map.empty))

          when {
            cache.getEntry[AmendVariationRenewalResponse](any())(any())
          } thenReturn Some(variationResponse.copy(
            addedResponsiblePeople = 1,
            addedResponsiblePeopleFitAndProper = 1,
            addedFullYearTradingPremises = 1,
            halfYearlyTradingPremises = 1,
            zeroRatedTradingPremises = 1
          ))

          whenReady(TestSubmissionResponseService.getVariation) {
            case Some((_, _, breakdownRows)) =>

              breakdownRows.head.label mustBe "confirmation.responsiblepeople"
              breakdownRows.head.quantity mustBe 1
              breakdownRows.head.perItm mustBe Currency(rpFee)
              breakdownRows.head.total mustBe Currency(rpFee)

              breakdownRows(1).label mustBe "confirmation.unpaidpeople"
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case (_, _, rows) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
            }
          }
        }

        "there is a Responsible People fee to pay and fpRate should be read dynamically" in new Fixture {

          val subscriptionResponseWithFeeRate = SubscriptionResponse(
            etmpFormBundleNumber = "",
            amlsRefNo = "amlsRef",Some(SubscriptionFees(
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople(), ResponsiblePeople()))

          val result = await(TestSubmissionResponseService.getSubscription)

          case class Test(str: String)

          result match {
            case (_, _, rows) => {
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
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case (_, _, rows) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          when {
            activities.businessActivities
          } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case (_, _, rows) => {
              rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
              rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
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
            cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(ResponsiblePeople()))

          val result = await(TestSubmissionResponseService.getSubscription)

          result match {
            case (_, _, rows) => rows foreach { row =>
              row.label must not equal "confirmation.responsiblepeople"
              row.label must not equal "confirmation.unpaidpeople"
            }
          }
        }
      }
    }
  }
}
