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

package services.businessactivities

import cats.implicits._
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, ExpectedAMLSTurnover}
import models.businessmatching.BusinessMatching
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExpectedAMLSTurnoverService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit
  ec: ExecutionContext
) {

  def getBusinessMatchingExpectedTurnover(
    credId: String
  ): Future[Option[(BusinessMatching, Option[ExpectedAMLSTurnover])]] =
    dataCacheConnector.fetchAll(credId) map { optCache =>
      optCache flatMap { cache =>
        (
          cache.getEntry[BusinessMatching](BusinessMatching.key),
          cache.getEntry[BusinessActivities](BusinessActivities.key)
        ) match {
          case (Some(bm), Some(ba)) => Some((bm, ba.expectedAMLSTurnover))
          case (Some(bm), None)     => Some((bm, None))
          case _                    => None
        }
      }
    }

  def getBusinessMatching(credId: String): Future[Option[BusinessMatching]] =
    dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key)

  def updateBusinessActivities(credId: String, expectedAMLSTurnover: ExpectedAMLSTurnover): Future[Option[Cache]] =
    dataCacheConnector.fetch[BusinessActivities](credId, BusinessActivities.key) map { baOpt =>
      baOpt map { ba =>
        dataCacheConnector
          .save[BusinessActivities](credId, BusinessActivities.key, ba.expectedAMLSTurnover(expectedAMLSTurnover))
      }
    } flatMap (_.sequence)
}
