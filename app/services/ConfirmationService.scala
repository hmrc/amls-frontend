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
import models.confirmation.BreakdownRow
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePeople
import models.status._
import models.tradingpremises.TradingPremises
import typeclasses.confirmation.BreakdownRowInstances._
import typeclasses.confirmation.BreakdownRows
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmationService @Inject()(
                                     val cacheConnector: DataCacheConnector
                                   ) extends DataCacheService {

  def getSubscription
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Seq[BreakdownRow]] =
    cacheConnector.fetchAll flatMap {
      maybeCache =>
        (for {
          cache <- maybeCache
          subscription <- cache.getEntry[SubscriptionResponse](SubscriptionResponse.key)
          premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
          people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          businessActivities <- businessMatching.activities
        } yield {
          Future.successful(BreakdownRows.generateBreakdownRows[SubmissionResponse](subscription, Some(businessActivities), Some(premises), Some(people)))
        }) getOrElse Future.failed(new Exception("Cannot get subscription response"))
    }

  def getAmendment
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[Seq[BreakdownRow]]] = {
    cacheConnector.fetchAll flatMap { maybeCache =>
      (for {
        cache <- maybeCache
        amendmentResponse <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
        premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
        people <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
        businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
        businessActivities <- businessMatching.activities
      } yield {
        val filteredPremises = TradingPremises.filter(premises)
        Future.successful(Some(
          BreakdownRows.generateBreakdownRows[SubmissionResponse](amendmentResponse, Some(businessActivities), Some(filteredPremises), Some(people))
        ))
      }) getOrElse OptionT.liftF(getSubscription).value
    }
  }

  def getVariation
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[Seq[BreakdownRow]]] = {
    cacheConnector.fetchAll flatMap {
      maybeCache =>
        (for {
          cache <- maybeCache
          variationResponse <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          businessActivities <- businessMatching.activities
        } yield {
          Future.successful(Some(
            BreakdownRows.generateBreakdownRows[AmendVariationRenewalResponse](variationResponse, Some(businessActivities), None, None)
          ))
        }) getOrElse Future.failed(new Exception("Cannot get subscription response"))
    }
  }

  def getRenewal
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[Option[Seq[BreakdownRow]]] = {
    cacheConnector.fetchAll flatMap {
      maybeCache =>
        (for {
          cache <- maybeCache
          renewal <- cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
        } yield {
          Future.successful(Some(
            BreakdownRows.generateBreakdownRows[AmendVariationRenewalResponse](renewal, None, None, None)
          ))
        }) getOrElse Future.failed(new Exception("Cannot get amendment response"))
    }
  }

  def isRenewalDefined(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Boolean] =
    cacheConnector.fetch[Renewal](Renewal.key).map(_.isDefined)

  def getBreakdownRows(status: SubmissionStatus, feeResponse: FeeResponse)
                      (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Option[Seq[BreakdownRow]]] =
    status match {
      case SubmissionReadyForReview if feeResponse.responseType equals AmendOrVariationResponseType => getAmendment.recover { case _ => None }
      case SubmissionDecisionApproved => getVariation
      case ReadyForRenewal(_) | RenewalSubmitted(_) => isRenewalDefined flatMap {
        case true => getRenewal
        case false => getVariation
      }
      case _ => getSubscription map (Some(_))
    }

}