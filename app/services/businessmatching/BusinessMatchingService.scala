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
import models.asp.Asp
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.status.{NotCompleted, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.tcsp.Tcsp
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BusinessMatchingService @Inject()(
                                         statusService: StatusService,
                                         dataCacheConnector: DataCacheConnector
                                       ) {

  def preApplicationComplete(implicit ac: AuthContext, hc: HeaderCarrier, ex: ExecutionContext): Future[Boolean] = {
    for {
      bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
    } yield bm.preAppComplete
  } getOrElse false

  def getModel(implicit ac:AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, BusinessMatching] = {
    lazy val originalModel = OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
    lazy val variationModel = OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.variationKey))

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
      case NotCompleted | SubmissionReady => OptionT.liftF(dataCacheConnector.save[BusinessMatching](BusinessMatching.key, model))
      case _ => OptionT.liftF(dataCacheConnector.save[BusinessMatching](BusinessMatching.variationKey, model))
    }

  }

  private def fetchActivitySet(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext) =
    for {
      viewResponse <- OptionT(dataCacheConnector.fetch[ViewResponse](ViewResponse.key))
      submitted <- OptionT.fromOption[Future](viewResponse.businessMatchingSection.activities)
      model <- getModel
      current <- OptionT.fromOption[Future](model.activities)
    } yield (current.businessActivities, current.removeActivities.fold(submitted.businessActivities) { removed =>
      submitted.businessActivities diff removed
    })

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
      case NotCompleted | SubmissionReady => OptionT(dataCacheConnector.fetchAll)
      case _ =>
        for {
          cacheMap <- OptionT(dataCacheConnector.fetchAll)
          primaryModel <- OptionT.fromOption[Future](cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          variationModel <- OptionT.fromOption[Future](cacheMap.getEntry[BusinessMatching](BusinessMatching.variationKey)) if variationModel != BusinessMatching()
          _ <- OptionT.liftF(dataCacheConnector.save[BusinessMatching](BusinessMatching.key, updateBusinessMatching(primaryModel, variationModel)))
          result <- clearVariation
        } yield result
    }
  }

  def clearVariation(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, CacheMap] =
    OptionT.liftF(dataCacheConnector.save[BusinessMatching](BusinessMatching.variationKey, BusinessMatching()))

  private def updateBusinessMatching(primaryModel: BusinessMatching, variationModel: BusinessMatching): BusinessMatching =
    (variationModel.activities match {
      case Some(BusinessActivities(existing, Some(additional), removed, doc)) =>
        variationModel.activities(BusinessActivities(existing ++ additional, None, removed, doc))
      case _ => variationModel.copy(hasChanged = primaryModel != variationModel)
    }).copy(hasAccepted = true)

  def activitiesToIterate(index: Int, activities: Set[BusinessActivity]) = activities.size > index + 1

  def clearSection(activity: BusinessActivity)(implicit ac: AuthContext, hc: HeaderCarrier) = activity match {
    case AccountancyServices => dataCacheConnector.save[Asp](Asp.key, None)
    case EstateAgentBusinessService => dataCacheConnector.save[EstateAgentBusiness](EstateAgentBusiness.key, None)
    case HighValueDealing => dataCacheConnector.save[Hvd](Hvd.key, None)
    case MoneyServiceBusiness => dataCacheConnector.save[Msb](Msb.key, None)
    case TrustAndCompanyServices => dataCacheConnector.save[Tcsp](Tcsp.key, None)
  }

}