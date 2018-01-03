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
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{Future, ExecutionContext}
import uk.gov.hmrc.http.{ HeaderCarrier, NotFoundException }

private[services] trait DataCacheService {

  private[services] def cacheConnector: DataCacheConnector

  def getCache
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[CacheMap] =
    cacheConnector.fetchAll flatMap {
      case Some(cache) =>
        Future.successful(cache)
      case None =>
        Future.failed {
          new NotFoundException("No CacheMap found for user")
        }
    }
}
