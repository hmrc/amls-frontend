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

package services.businessmatching

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.ViewResponse
import models.asp.Asp
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.tcsp.Tcsp
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessMatchingService @Inject() (dataCacheConnector: DataCacheConnector) {

  def preApplicationComplete(credId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    for {
      bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
    } yield bm.preAppComplete
  } getOrElse false

  def getModel(credId: String): OptionT[Future, BusinessMatching] =
    OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))

  def updateModel(credId: String, model: BusinessMatching)(implicit ec: ExecutionContext): OptionT[Future, Cache] =
    OptionT.liftF(dataCacheConnector.save[BusinessMatching](credId, BusinessMatching.key, model))

  private def fetchActivitySet(cacheId: String)(implicit ec: ExecutionContext) =
    for {
      viewResponse <- OptionT(dataCacheConnector.fetch[ViewResponse](cacheId, ViewResponse.key))
      submitted    <- OptionT.fromOption[Future](viewResponse.businessMatchingSection.activities)
      model        <- getModel(cacheId)
      current      <- OptionT.fromOption[Future](model.activities)
    } yield (
      current.businessActivities,
      current.removeActivities.fold(submitted.businessActivities) { removed =>
        submitted.businessActivities diff removed
      }
    )

  private def getActivitySet(
    cacheId: String,
    fn: (Set[BusinessActivity], Set[BusinessActivity]) => Set[BusinessActivity]
  )(implicit ec: ExecutionContext): OptionT[Future, Set[BusinessActivity]] =
    fetchActivitySet(cacheId) map fn.tupled

  def getAdditionalBusinessActivities(cacheId: String)(implicit
    ec: ExecutionContext
  ): OptionT[Future, Set[BusinessActivity]] =
    getActivitySet(cacheId, _ diff _)

  def getSubmittedBusinessActivities(credId: String)(implicit
    ec: ExecutionContext
  ): OptionT[Future, Set[BusinessActivity]] =
    getActivitySet(credId, _ intersect _)

  def getRemainingBusinessActivities(
    credId: String
  )(implicit ec: ExecutionContext): OptionT[Future, Set[BusinessActivity]] =
    for {
      model      <- getModel(credId)
      activities <- OptionT.fromOption[Future](model.activities)
    } yield BusinessActivities.all diff activities.businessActivities

  def clearSection(credId: String, activity: BusinessActivity): Future[Cache] = activity match {
    case AccountancyServices        =>
      dataCacheConnector.removeByKey(credId, Asp.key)
    case EstateAgentBusinessService =>
      dataCacheConnector.removeByKey(credId, Eab.key)
    case HighValueDealing           =>
      dataCacheConnector.removeByKey(credId, Hvd.key)
    case MoneyServiceBusiness       =>
      dataCacheConnector.removeByKey(credId, Msb.key)
    case TrustAndCompanyServices    =>
      dataCacheConnector.removeByKey(credId, Tcsp.key)
    case _                          => throw new Exception("An Unknown Exception has occurred : BusinessMatchingService")
  }

}
