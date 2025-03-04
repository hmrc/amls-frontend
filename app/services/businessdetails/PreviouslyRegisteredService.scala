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

package services.businessdetails

import connectors.DataCacheConnector
import models.businessdetails.{BusinessDetails, PreviouslyRegistered}
import play.api.Logging
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviouslyRegisteredService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit ec: ExecutionContext)
    extends Logging {

  def getPreviouslyRegistered(credId: String): Future[Option[PreviouslyRegistered]] =
    dataCacheConnector.fetch[BusinessDetails](credId, BusinessDetails.key) map { optBusinessDetails =>
      optBusinessDetails.flatMap(_.previouslyRegistered)
    }

  def updatePreviouslyRegistered(credId: String, data: PreviouslyRegistered): Future[Option[Cache]] = {

    val businessDetailsOptF = dataCacheConnector.fetchAll(credId) map {
      _.map { cache =>
        cache
          .getEntry[BusinessDetails](BusinessDetails.key)
          .getOrElse(BusinessDetails())
          .copy(previouslyRegistered = Some(data), hasChanged = true)
      }
    }

    val logPrefix = "[PreviouslyRegisteredService][updatePreviouslyRegistered]"

    businessDetailsOptF flatMap { detailsOpt =>
      dataCacheConnector.save[BusinessDetails](
        credId,
        BusinessDetails.key,
        detailsOpt
      ) map { cache =>
        logger.info(s"$logPrefix: Business details updated successfully")
        Some(cache)
      }
    } recover { case _: Exception =>
      logger.warn(s"$logPrefix: Failed to update Business details")
      None
    }
  }
}
