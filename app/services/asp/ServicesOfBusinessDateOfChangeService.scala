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

package services.asp

import connectors.DataCacheConnector
import models.DateOfChange
import models.asp.Asp
import models.businessdetails.{ActivityStartDate, BusinessDetails}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ServicesOfBusinessDateOfChangeService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit
  ec: ExecutionContext
) {

  def getModelWithDate(cacheId: String): Future[(Asp, Option[ActivityStartDate])] =
    dataCacheConnector.fetchAll(cacheId) map { optionalCache =>
      (for {
        cache           <- optionalCache
        businessDetails <- cache.getEntry[BusinessDetails](BusinessDetails.key)
        asp             <- cache.getEntry[Asp](Asp.key)
      } yield (asp, businessDetails.activityStartDate)) match {
        case Some((asp, Some(activityStartDate))) => (asp, Some(activityStartDate))
        case Some((asp, _))                       => (asp, None)
        case _                                    => (Asp(), None)
      }
    }

  def updateAsp(asp: Asp, dateOfChange: DateOfChange, credId: String): Future[Option[Asp]] = {
    val updatedAsp = asp.services match {
      case Some(sob) => asp.copy(services = Some(sob.copy(dateOfChange = Some(dateOfChange))))
      case None      => asp
    }

    if (updatedAsp != asp) {
      dataCacheConnector.save[Asp](credId, Asp.key, updatedAsp).map(_.getEntry[Asp](Asp.key))
    } else {
      Future.successful(Some(asp))
    }
  }
}
