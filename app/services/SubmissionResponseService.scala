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

import cats.implicits._
import cats.data.OptionT
import config.ApplicationConfig
import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching, TrustAndCompanyServices, MoneyServiceBusiness => MSB}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview}
import models.tradingpremises.TradingPremises
import models.{AmendVariationRenewalResponse, SubmissionResponse, SubscriptionResponse}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.StatusConstants

import scala.concurrent.{ExecutionContext, Future}

sealed case class RowEntity(message: String, feePer: BigDecimal)

trait SubmissionResponseService extends FeeCalculations with DataCacheService {

  private[services] val statusService: StatusService

  type SubmissionData = (Option[String], Currency, Seq[BreakdownRow], Either[String, Option[Currency]])

  def getSubscription
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[SubmissionData] =
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          subscription <- cache.getEntry[SubscriptionResponse](SubscriptionResponse.key)
          premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
          people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          businessActivities <- businessMatching.activities
        } yield {
          val subQuantity = subscriptionQuantity(subscription)
          val paymentReference = subscription.getPaymentReference
          val total = subscription.getTotalFees
          val rows = getBreakdownRows(subscription, premises, people, businessActivities, subQuantity)
          val amlsRefNo = subscription.amlsRefNo
          Future.successful((paymentReference.some, Currency.fromBD(total), rows, Left(amlsRefNo)))
        }) getOrElse Future.failed(new Exception("Cannot get subscription response"))
    }

  def getAmendment
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[(Option[String], Currency, Seq[BreakdownRow], Option[Currency])]] = {
    cacheConnector.fetchAll flatMap {
      getDataForAmendment(_) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  def getVariation
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[(Option[String], Currency, Seq[BreakdownRow], Option[Currency])]] = {
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          variationResponse <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          businessActivities <- businessMatching.activities
        } yield {
          val paymentReference = variationResponse.paymentReference
          val total = variationResponse.getTotalFees
          val rows = getVariationBreakdownRows(variationResponse, businessActivities)
          val difference = variationResponse.difference map Currency.fromBD
          Future.successful(Some((paymentReference, Currency.fromBD(total), rows, difference)))
        }) getOrElse Future.failed(new Exception("Cannot get subscription response"))
    }
  }

  def getRenewal
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[(Option[String], Currency, Seq[BreakdownRow], Option[Currency])]] = {
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          renewal <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
        } yield {
          val totalFees: BigDecimal = renewal.getTotalFees
          val rows = getRenewalBreakdown(renewal)
          val paymentRef = renewal.paymentReference
          val difference = renewal.difference
          Future.successful(Some((paymentRef, Currency(totalFees), rows, difference map Currency.fromBD)))
        }) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  private def subscriptionQuantity(subscription: SubmissionResponse): Int =
    if (subscription.getRegistrationFee == 0) 0 else 1

  private def getDataForAmendment(option: Option[CacheMap])(implicit authContent: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) = {
    for {
      cache <- option
      amendmentResponse <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
      premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
      people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
      businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      businessActivities <- businessMatching.activities
    } yield {
      val subQuantity = subscriptionQuantity(amendmentResponse)
      val total = amendmentResponse.totalFees
      val difference = amendmentResponse.difference map Currency.fromBD
      val filteredPremises = premises.filter(!_.status.contains(StatusConstants.Deleted))
      val rows = getBreakdownRows(amendmentResponse, filteredPremises, people, businessActivities, subQuantity)
      val paymentRef = amendmentResponse.paymentReference
      Future.successful(Some((paymentRef, Currency.fromBD(total), rows, difference)))
    }
  }

  private def getRenewalBreakdown(renewal: AmendVariationRenewalResponse): Seq[BreakdownRow] = {

    val breakdownRows = Seq.empty

    def renewalRow(count: Int, rowEntity: RowEntity, total: AmendVariationRenewalResponse => BigDecimal): Seq[BreakdownRow] = {
      if (count > 0) {
        breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(renewal))))
      } else {
        Seq.empty
      }
    }

    def rpRow: Seq[BreakdownRow] = renewalRow(renewal.addedResponsiblePeople, peopleVariationRow(renewal), renewalPeopleFee)

    def fpRow: Seq[BreakdownRow] = renewalRow(renewal.addedResponsiblePeopleFitAndProper, peopleFPPassed, renewalFitAndProperDeduction)

    def tpFullYearRow: Seq[BreakdownRow] = renewalRow(renewal.addedFullYearTradingPremises, premisesVariationRow(renewal), fullPremisesFee)

    def tpHalfYearRow: Seq[BreakdownRow] = renewalRow(renewal.halfYearlyTradingPremises, premisesHalfYear(renewal), renewalHalfYearPremisesFee)

    def tpZeroRow: Seq[BreakdownRow] = renewalRow(renewal.zeroRatedTradingPremises, PremisesZero, renewalZeroPremisesFee)

    rpRow ++ fpRow ++ tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow

  }

  private def getBreakdownRows
  (submission: SubmissionResponse,
   premises: Seq[TradingPremises],
   people: Seq[ResponsiblePeople],
   businessActivities: BusinessActivities,
   subQuantity: Int): Seq[BreakdownRow] = {
    Seq(
      BreakdownRow(submissionRow(submission).message, subQuantity, submissionRow(submission).feePer, subQuantity * submissionRow(submission).feePer)
    ) ++ responsiblePeopleRows(people, submission, businessActivities.businessActivities) ++ Seq(
      BreakdownRow(premisesRow(submission).message, premises.size, premisesRow(submission).feePer, submission.getPremiseFee)
    )
  }

  private def getVariationBreakdownRows
  (variationResponse: AmendVariationRenewalResponse,
   businessActivities: BusinessActivities): Seq[BreakdownRow] = {
    responsiblePeopleVariationRows(variationResponse, businessActivities.businessActivities) ++ tradingPremisesVariationRows(variationResponse)
  }

  private def responsiblePeopleRows
  (people: Seq[ResponsiblePeople],
   subscription: SubmissionResponse,
   activities: Set[BusinessActivity]): Seq[BreakdownRow] = {
    if (showBreakdown(subscription.getFpFee, activities)) {

      splitPeopleByFitAndProperTest(people) match {
        case (passedFP, notFP) =>
          Seq(
            BreakdownRow(peopleRow(subscription).message, notFP.size, peopleRow(subscription).feePer, Currency.fromBD(subscription.getFpFee.getOrElse(0)))
          ) ++ (if (passedFP.nonEmpty) {
            Seq(
              BreakdownRow(peopleFPPassed.message, passedFP.size, max(0, peopleFPPassed.feePer), Currency.fromBD(max(0, peopleFPPassed.feePer)))
            )
          } else {
            Seq.empty
          })
      }
    } else {
      Seq.empty
    }
  }

  private def tradingPremisesVariationRows(variationRenewalResponse: AmendVariationRenewalResponse): Seq[BreakdownRow] = {
    val breakdownRows = Seq.empty

    def variationRow(count: Int, rowEntity: RowEntity, total: AmendVariationRenewalResponse => BigDecimal): Seq[BreakdownRow] = {
      if (count > 0) {
        breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(variationRenewalResponse))))
      } else {
        Seq.empty
      }
    }

    def tpFullYearRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.addedFullYearTradingPremises,
      premisesVariationRow(variationRenewalResponse),
      fullPremisesFee
    )

    def tpHalfYearRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.halfYearlyTradingPremises,
      premisesHalfYear(variationRenewalResponse),
      renewalHalfYearPremisesFee
    )

    def tpZeroRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.zeroRatedTradingPremises,
      PremisesZero,
      renewalZeroPremisesFee
    )

    tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow
  }

  private def responsiblePeopleVariationRows
  (variationResponse: AmendVariationRenewalResponse,
   activities: Set[BusinessActivity]): Seq[BreakdownRow] = {

    if (showBreakdown(variationResponse.getFpFee, activities)) {

      val (passedFP, notFP) = (variationResponse.addedResponsiblePeopleFitAndProper, variationResponse.addedResponsiblePeople)

      (if (notFP > 0) {
        Seq(
          BreakdownRow(peopleVariationRow(variationResponse).message, notFP, peopleVariationRow(variationResponse).feePer, Currency.fromBD(variationResponse.getFpFee.getOrElse(0)))
        )
      } else {
        Seq.empty
      }) ++ (if (passedFP > 0) {
        Seq(BreakdownRow(peopleFPPassed.message, passedFP, max(0, peopleFPPassed.feePer), Currency.fromBD(max(0, peopleFPPassed.feePer))))
      } else {
        Seq.empty
      })

    } else {
      Seq.empty
    }
  }

  private def getAmendmentFees(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Option[SubmissionData]] = {
    getAmendment flatMap {
      case Some((paymentRef, total, rows, difference)) =>
        Future.successful(
          (difference, paymentRef) match {
            case (Some(currency), Some(payRef)) if currency.value > 0 => Some((paymentRef, total, rows, Right(difference)))
            case _ => None
          }
        )
      case None => Future.failed(new Exception("Cannot get data from amendment submission"))
    }
  }

  private def getRenewalOrVariationData(getData: Future[Option[(Option[String], Currency, Seq[BreakdownRow], Option[Currency])]])
                                       (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Option[SubmissionData]] = {
    getData flatMap {
      case Some((paymentRef, total, rows, difference)) => Future.successful(
        paymentRef match {
          case Some(payRef) if total.value > 0 => Some((paymentRef, total, rows, Right(difference)))
          case _ => None
        })
      case None => Future.failed(new Exception("Cannot get data from submission"))
    }
  }

  def getSubmissionData(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Option[SubmissionData]] = {
    statusService.getStatus flatMap {
      case SubmissionReadyForReview => getAmendmentFees
      case SubmissionDecisionApproved => getRenewalOrVariationData(getVariation)
      case ReadyForRenewal(_) => getRenewalOrVariationData(getRenewal)
      case _ => getSubscription map {
        case (payRef, total, breakdown, amlsRefNo@Left(_)) => (payRef, total, breakdown, amlsRefNo).some
      }
    }
  }

  private val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y

  private val showBreakdown = (fpFee: Option[BigDecimal], activities: Set[BusinessActivity]) =>
    fpFee.fold(activities.exists(act => act == MSB || act == TrustAndCompanyServices)) { _ => true }

  private val splitPeopleByFitAndProperTest = (people: Seq[ResponsiblePeople]) =>
    people.filter(!_.status.contains(StatusConstants.Deleted)).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false))

}

object SubmissionResponseService extends SubmissionResponseService {
  // $COVERAGE-OFF$
  override private[services] val cacheConnector = DataCacheConnector
  override private[services] val statusService = StatusService
}

sealed trait FeeCalculations {

  def submissionRow(response: SubmissionResponse) = RowEntity("confirmation.submission", response.getRegistrationFee)

  def premisesRow(response: SubmissionResponse) = RowEntity("confirmation.tradingpremises",
    response.getPremiseFeeRate.getOrElse(ApplicationConfig.premisesFee))

  def premisesVariationRow(variationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.tradingpremises",
    variationResponse.getPremiseFeeRate.getOrElse(ApplicationConfig.premisesFee))

  def premisesHalfYear(response: SubmissionResponse) = RowEntity("confirmation.tradingpremises.half",
    premisesRow(response).feePer / 2)

  def renewalPremisesHalfYear(rvariationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.tradingpremises.half",
    premisesVariationRow(rvariationResponse).feePer / 2)

  val PremisesZero = RowEntity("confirmation.tradingpremises.zero", 0)

  def peopleRow(response: SubmissionResponse) = RowEntity("confirmation.responsiblepeople",
    response.getFpFeeRate.getOrElse(ApplicationConfig.peopleFee))

  def peopleVariationRow(variationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.responsiblepeople",
    variationResponse.getFpFeeRate.getOrElse(ApplicationConfig.peopleFee))

  def renewalTotalPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    (premisesRow(renewal).feePer * renewal.addedFullYearTradingPremises) + renewalHalfYearPremisesFee(renewal)

  def fullPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    premisesVariationRow(renewal).feePer * renewal.addedFullYearTradingPremises

  def renewalHalfYearPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    renewalPremisesHalfYear(renewal).feePer * renewal.halfYearlyTradingPremises

  def renewalPeopleFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    peopleVariationRow(renewal).feePer * renewal.addedResponsiblePeople

  def renewalFitAndProperDeduction(renewal: AmendVariationRenewalResponse): BigDecimal = 0

  def renewalZeroPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal = 0

  val peopleFPPassed = RowEntity("confirmation.responsiblepeople.fp.passed", 0)

}