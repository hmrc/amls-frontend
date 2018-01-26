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

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.ResponseType.AmendOrVariationResponseType
import models._
import models.businessmatching.BusinessMatching
import models.confirmation.{Currency, SubmissionData}
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePeople
import models.status._
import models.tradingpremises.TradingPremises
import typeclasses.confirmation.BreakdownRowInstances._
import typeclasses.confirmation.BreakdownRows
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

sealed case class RowEntity(message: String, feePer: BigDecimal)

@Singleton
class SubmissionResponseService @Inject()(
                                           val cacheConnector: DataCacheConnector
                                         ) extends DataCacheService {

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
          val paymentReference = subscription.getPaymentReference
          val total = subscription.getTotalFees
          val rows = BreakdownRows.generateBreakdownRows[SubmissionResponse](subscription, Some(businessActivities), Some(premises), Some(people))
          val amlsRefNo = subscription.amlsRefNo
          Future.successful(SubmissionData(paymentReference.some, Currency.fromBD(total), rows, Some(amlsRefNo), None))
        }) getOrElse Future.failed(new Exception("Cannot get subscription response"))
    }

  def getAmendment
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[SubmissionData]] = {
    cacheConnector.fetchAll flatMap { option =>
      (for {
        cache <- option
        amendmentResponse <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
        premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
        people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
        businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
        businessActivities <- businessMatching.activities
      } yield {
        val total = amendmentResponse.totalFees
        val difference = amendmentResponse.difference map Currency.fromBD
        val filteredPremises = TradingPremises.filter(premises)
        val rows = BreakdownRows.generateBreakdownRows[SubmissionResponse](amendmentResponse, Some(businessActivities), Some(filteredPremises), Some(people))
        val paymentRef = amendmentResponse.paymentReference
        Future.successful(Some(SubmissionData(paymentRef, Currency.fromBD(total), rows, None, difference)))
      }) getOrElse OptionT.liftF(getSubscription).value
    }
  }

  def getVariation
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[SubmissionData]] = {
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
          val rows = BreakdownRows.generateBreakdownRows[AmendVariationRenewalResponse](variationResponse, Some(businessActivities), None, None)
          val difference = variationResponse.difference map Currency.fromBD
          Future.successful(Some(SubmissionData(paymentReference, Currency.fromBD(total), rows, None, difference)))
        }) getOrElse Future.failed(new Exception("Cannot get subscription response"))
    }
  }

  def getRenewal
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[SubmissionData]] = {
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          renewal <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
        } yield {
          val totalFees: BigDecimal = renewal.getTotalFees
          val rows = BreakdownRows.generateBreakdownRows[AmendVariationRenewalResponse](renewal, None, None, None)
          val paymentRef = renewal.paymentReference
          val difference = renewal.difference
          Future.successful(Some(SubmissionData(paymentRef, Currency(totalFees), rows, None, difference map Currency.fromBD)))
        }) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  def isRenewalDefined(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Boolean] =
    cacheConnector.fetch[Renewal](Renewal.key).map(_.isDefined)

  def getSubmissionData(status: SubmissionStatus, feeResponse: Option[FeeResponse])
                       (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Option[SubmissionData]] = {

    val responseType = feeResponse.map(_.responseType)

    status match {
      case SubmissionReadyForReview if responseType contains AmendOrVariationResponseType => getAmendment
      case SubmissionDecisionApproved => getVariation
      case ReadyForRenewal(_) | RenewalSubmitted(_) => isRenewalDefined flatMap {
        case true => getRenewal
        case false => getVariation
      }
      case _ => getSubscription map (Some(_))
    }
  }

}