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

package services.businessmatching

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.ViewResponse
import models.businessmatching._
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class BusinessMatchingService @Inject()(
                                         statusService: StatusService,
                                         cache: DataCacheConnector
                                       ) {

  def getModel(implicit ac:AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, BusinessMatching] = {
    lazy val originalModel = OptionT(cache.fetch[BusinessMatching](BusinessMatching.key))
    lazy val variationModel = OptionT(cache.fetch[BusinessMatching](BusinessMatching.variationKey))

    OptionT.liftF(statusService.getStatus) flatMap {
      case NotCompleted | SubmissionReady => originalModel
      case _ => variationModel collect {
        case x if !x.equals(BusinessMatching()) => x
      } orElse originalModel
    }
  }

  def updateModel(model: BusinessMatching)
                 (implicit ac:AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, CacheMap] = {

    OptionT.liftF(statusService.getStatus) flatMap {
      case NotCompleted | SubmissionReady => OptionT.liftF(cache.save[BusinessMatching](BusinessMatching.key, model))
      case _ => OptionT.liftF(cache.save[BusinessMatching](BusinessMatching.variationKey, model))
    }

  }

  private def fetchActivitySet(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) =
    for {
      viewResponse <- OptionT(cache.fetch[ViewResponse](ViewResponse.key))
      submitted <- OptionT.fromOption[Future](viewResponse.businessMatchingSection.activities)
      model <- getModel
      current <- OptionT.fromOption[Future](model.activities)
    } yield (current.businessActivities, submitted.businessActivities)

  private def getActivitySet(fn: (Set[BusinessActivity], Set[BusinessActivity]) => Set[BusinessActivity])
                            (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Set[BusinessActivity]] =
    fetchActivitySet map fn.tupled

  def getAdditionalBusinessActivities(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Set[BusinessActivity]] =
    getActivitySet(_ diff _)

  def getSubmittedBusinessActivities(implicit ac: AuthContext, hc: HeaderCarrier, ex: ExecutionContext): OptionT[Future, Set[BusinessActivity]] =
    getActivitySet(_ intersect _)

  def fitAndProperRequired(implicit ac: AuthContext, hc: HeaderCarrier, ex: ExecutionContext): OptionT[Future, Boolean] =
    fetchActivitySet map { case (current, existing) =>
      !((existing contains TrustAndCompanyServices) | (existing contains MoneyServiceBusiness)) &
        (current contains TrustAndCompanyServices) | (current contains MoneyServiceBusiness)
    }

  def commitVariationData(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, CacheMap] = {
    OptionT.liftF(statusService.getStatus) flatMap {
      case NotCompleted | SubmissionReady => OptionT(cache.fetchAll)
      case _ =>
        for {
          cacheMap <- OptionT(cache.fetchAll)
          primaryModel <- OptionT.fromOption[Future](cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          variationModel <- OptionT.fromOption[Future](cacheMap.getEntry[BusinessMatching](BusinessMatching.variationKey)) if variationModel != BusinessMatching()
          _ <- OptionT.liftF(cache.save[BusinessMatching](BusinessMatching.key, updateBusinessMatching(primaryModel, variationModel)))
          result <- clearVariation
        } yield result
    }
  }

  def clearVariation(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, CacheMap] =
    OptionT.liftF(cache.save[BusinessMatching](BusinessMatching.variationKey, BusinessMatching()))

  private def updateBusinessMatching(primaryModel: BusinessMatching, variationModel: BusinessMatching): BusinessMatching =
    variationModel.activities match {
      case Some(BusinessActivities(existing, Some(additional))) => variationModel.activities(BusinessActivities(existing ++ additional, None))
      case _ => variationModel.copy(hasChanged = primaryModel != variationModel)
    }

}