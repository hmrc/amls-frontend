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
import models.businessmatching.BusinessMatching
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class BusinessMatchingService @Inject()(statusService: StatusService, cache: DataCacheConnector) {
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

  def commitVariationData(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, CacheMap] = for {
    cacheMap <- OptionT(cache.fetchAll)
    variation <- OptionT.fromOption[Future](cacheMap.getEntry[BusinessMatching](BusinessMatching.variationKey))
    _ <- OptionT.liftF(cache.save(BusinessMatching.key, variation))
    result <- clearVariation
  } yield result

  def clearVariation(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, CacheMap] =
    OptionT.liftF(cache.save[BusinessMatching](BusinessMatching.variationKey, BusinessMatching()))
}
