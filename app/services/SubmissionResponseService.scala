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

import config.ApplicationConfig
import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities => BusinessSevices, BusinessMatching, MoneyServiceBusiness => MSB, TrustAndCompanyServices}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import models.{AmendVariationRenewalResponse, SubmissionResponse, SubscriptionResponse}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.StatusConstants

import scala.concurrent.{ExecutionContext, Future}

private case class RowEntity(message: String, feePer: BigDecimal)

trait SubmissionResponseService extends Rows with DataCacheService {

  def getSubscription
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[(String, Currency, Seq[BreakdownRow])] =
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
          Future.successful((paymentReference, Currency.fromBD(total), rows))
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
  ): Future[(String, Currency, Seq[BreakdownRow])] =
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          variation <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
          premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
          people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          businessActivities <- businessMatching.activities
        } yield {
          val paymentReference = variation.getPaymentReference
          val total = variation.getTotalFees
          val rows = getVariationBreakdownRows(variation, premises, people, businessActivities)
          Future.successful((paymentReference, Currency.fromBD(total), rows))
        }) getOrElse Future.failed(new Exception("Cannot get subscription response"))
    }

  def getRenewal
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[(Option[String], Currency, Seq[BreakdownRow])]] = {
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          renewal <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
        } yield {
          val premisesFee: BigDecimal = getRenewalTotalPremisesFee(renewal)
          val peopleFee: BigDecimal = getRenewalPeopleFee(renewal)
          val totalFees: BigDecimal = peopleFee + premisesFee
          val rows = getRenewalBreakdown(renewal, peopleFee)
          val paymentRef = renewal.paymentReference
          Future.successful(Some((paymentRef, Currency(totalFees), rows)))
        }) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  private def subscriptionQuantity(subscription: SubmissionResponse): Int =
    if (subscription.getRegistrationFee == 0) 0 else 1

  private def getDataForAmendment(option: Option[CacheMap])(implicit authContent: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) = {
    for {
      cache <- option
      amendment <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
      premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
      people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
      businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      businessActivities <- businessMatching.activities
    } yield {
      val subQuantity = subscriptionQuantity(amendment)
      val total = amendment.totalFees
      val difference = amendment.difference map Currency.fromBD
      val filteredPremises = premises.filter(!_.status.contains(StatusConstants.Deleted))
      val rows = getBreakdownRows(amendment, filteredPremises, people, businessActivities, subQuantity)
      val paymentRef = amendment.paymentReference
      Future.successful(Some((paymentRef, Currency.fromBD(total), rows, difference)))
    }
  }

  private def getRenewalBreakdown(renewal: AmendVariationRenewalResponse, peopleFee: BigDecimal): Seq[BreakdownRow] = {

    val breakdownRows = Seq()

    def renewalRow(count: Int, rowEntity: RowEntity, total: AmendVariationRenewalResponse => BigDecimal): Seq[BreakdownRow] = {
      if (count > 0) {
        breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(renewal))))
      } else {
        Seq()
      }
    }

    def rpRow: Seq[BreakdownRow] = renewalRow(renewal.addedResponsiblePeople, peopleRow(renewal), getRenewalPeopleFee)
    def fpRow: Seq[BreakdownRow] = renewalRow(renewal.addedResponsiblePeopleFitAndProper, responsiblePeopleFPPassed, getRenewalFitAndProperDeduction)

    def tpFullYearRow: Seq[BreakdownRow] = renewalRow(renewal.addedFullYearTradingPremises, premisesRow(renewal), getRenewalFullPremisesFee)
    def tpHalfYearRow: Seq[BreakdownRow] = renewalRow(renewal.halfYearlyTradingPremises, premisesHalfYear(renewal), getRenewalHalfYearPremisesFee)
    def tpZeroRow: Seq[BreakdownRow] = renewalRow(renewal.zeroRatedTradingPremises, PremisesZero, getRenewalZeroPremisesFee)

    rpRow ++ fpRow ++ tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow

  }

  private def getRenewalTotalPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    (premisesRow(renewal).feePer * renewal.addedFullYearTradingPremises) + getRenewalHalfYearPremisesFee(renewal)

  private def getRenewalFullPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    premisesRow(renewal).feePer * renewal.addedFullYearTradingPremises

  private def getRenewalHalfYearPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    premisesHalfYear(renewal).feePer * renewal.halfYearlyTradingPremises

  private def getRenewalPeopleFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    peopleRow(renewal).feePer * renewal.addedResponsiblePeople

  private def getRenewalFitAndProperDeduction(renewal: AmendVariationRenewalResponse): BigDecimal = 0

  private def getRenewalZeroPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal = 0

  private def getHalfYearPremisesFee(variation: AmendVariationRenewalResponse): BigDecimal =
    premisesHalfYear(variation).feePer * variation.halfYearlyTradingPremises

  private def getBreakdownRows
  (submission: SubmissionResponse,
   premises: Seq[TradingPremises],
   people: Seq[ResponsiblePeople],
   businessActivities: BusinessSevices,
   subQuantity: Int): Seq[BreakdownRow] = {
    Seq(BreakdownRow(submissionRow(submission).message, subQuantity,
      submissionRow(submission).feePer, subQuantity * submissionRow(submission).feePer)) ++
      responsiblePeopleRows(people, submission, businessActivities) ++
      Seq(BreakdownRow(premisesRow(submission).message, premises.size, premisesRow(submission).feePer, submission.getPremiseFee))
  }

  private def getVariationBreakdownRows
  (variation: AmendVariationRenewalResponse,
   premises: Seq[TradingPremises],
   people: Seq[ResponsiblePeople],
   businessActivities: BusinessSevices): Seq[BreakdownRow] = {
    responsiblePeopleVariationRows(people, variation, businessActivities) ++
    tradingPremisesVariationRows(premises,variation)
  }

  private def responsiblePeopleRows(
                                     people: Seq[ResponsiblePeople],
                                     subscription: SubmissionResponse,
                                     businessActivities: BusinessSevices
                                   ): Seq[BreakdownRow] = {

    val showBreakdown = subscription.getFpFee match {
      case None => businessActivities.businessActivities.exists(act => act == MSB || act == TrustAndCompanyServices)
      case _ => true
    }

    if (showBreakdown) {

      val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y

      people.filter(!_.status.contains(StatusConstants.Deleted)).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false)) match {
        case (b, a) =>
          Seq(BreakdownRow(peopleRow(subscription).message, a.size, peopleRow(subscription).feePer,
            Currency.fromBD(subscription.getFpFee.getOrElse(0)))) ++
            (if (b.nonEmpty) {
              Seq(BreakdownRow(responsiblePeopleFPPassed.message, b.size, max(0, responsiblePeopleFPPassed.feePer), Currency.fromBD(max(0, responsiblePeopleFPPassed.feePer))))
            } else {
              Seq.empty
            })
      }
    } else {
      Seq.empty
    }
  }

  private def tradingPremisesVariationRows(
                                          premises: Seq[TradingPremises],
                                          variationRenewalResponse: AmendVariationRenewalResponse
                                          ): Seq[BreakdownRow] = {
    val breakdownRows = Seq()

    def variationRow(count: Int, rowEntity: RowEntity, total: AmendVariationRenewalResponse => BigDecimal): Seq[BreakdownRow] = {
      if (count > 0) {
        breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(variationRenewalResponse))))
      } else {
        Seq()
      }
    }

    def tpFullYearRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.addedFullYearTradingPremises,
      premisesRowVariation(variationRenewalResponse),
      getRenewalFullPremisesFee
    )

    def tpHalfYearRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.halfYearlyTradingPremises,
      premisesHalfYear(variationRenewalResponse),
      getRenewalHalfYearPremisesFee
    )

    def tpZeroRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.zeroRatedTradingPremises,
      PremisesZero,
      getRenewalZeroPremisesFee
    )

    tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow
  }

  private def responsiblePeopleVariationRows(
                                     people: Seq[ResponsiblePeople],
                                     variation: AmendVariationRenewalResponse,
                                     businessActivities: BusinessSevices
                                   ): Seq[BreakdownRow] = {

    val showBreakdown = variation.getFpFee match {
      case None => businessActivities.businessActivities.exists(act => act == MSB || act == TrustAndCompanyServices)
      case _ => true
    }

    if (showBreakdown) {

      val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y
      people.filter(!_.status.contains(StatusConstants.Deleted)).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false)) match {
        case (passed, feeRqd) =>
          println("passed " + passed + "feeRqd " + feeRqd)
          (if (feeRqd.nonEmpty) {
            Seq(BreakdownRow(peopleRowVariation(variation).message, feeRqd.size, peopleRowVariation(variation).feePer,
              Currency.fromBD(variation.getFpFee.getOrElse(0))))
          } else {
            Seq.empty
          }) ++
            (if(passed.nonEmpty) {
              Seq(BreakdownRow(responsiblePeopleFPPassed.message, passed.size, max(0, responsiblePeopleFPPassed.feePer), Currency.fromBD(max(0, responsiblePeopleFPPassed.feePer))))
            } else {
              Seq.empty
            })
      }
    } else {
      Seq.empty
    }
  }

}

object SubmissionResponseService extends SubmissionResponseService {
  // $COVERAGE-OFF$
  override private[services] val cacheConnector = DataCacheConnector
}

trait Rows {

  def submissionRow(response: SubmissionResponse) = RowEntity("confirmation.submission", response.getRegistrationFee)

  def premisesRow(response: SubmissionResponse) = RowEntity("confirmation.tradingpremises",
    response.getPremiseFeeRate.getOrElse(ApplicationConfig.premisesFee))

  def premisesRowVariation(variationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.tradingpremises",
    variationResponse.getPremiseFeeRate.getOrElse(ApplicationConfig.premisesFee))

  def premisesHalfYear(response: SubmissionResponse) = RowEntity("confirmation.tradingpremises.half",
    premisesRow(response).feePer / 2)

  val PremisesZero = RowEntity("confirmation.tradingpremises.zero", 0)

  def peopleRow(response: SubmissionResponse) = RowEntity("confirmation.responsiblepeople",
    response.getFpFeeRate.getOrElse(ApplicationConfig.peopleFee))

  def peopleRowVariation(variationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.responsiblepeople",
    variationResponse.getFpFeeRate.getOrElse(ApplicationConfig.peopleFee))

  val responsiblePeopleFPPassed = RowEntity("confirmation.responsiblepeople.fp.passed", 0)

}