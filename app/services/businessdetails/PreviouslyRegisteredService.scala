/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits._
import connectors.DataCacheConnector
import models.businessdetails.{BusinessDetails, PreviouslyRegistered}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviouslyRegisteredService @Inject()(val dataCacheConnector: DataCacheConnector)(implicit ec: ExecutionContext) {

  def getPreviouslyRegistered(credId: String)(implicit hc: HeaderCarrier): Future[Option[PreviouslyRegistered]] = {
    dataCacheConnector.fetch[BusinessDetails](credId, BusinessDetails.key) map { optBusinessDetails =>
      optBusinessDetails.flatMap(_.previouslyRegistered)
    }
  }

  def updatePreviouslyRegistered(credId: String, data: PreviouslyRegistered)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    dataCacheConnector.fetchAll(credId) map {
      _.flatMap { cache =>
        cache.getEntry[BusinessDetails](BusinessDetails.key) map { businessDetails =>
          dataCacheConnector.save[BusinessDetails](
            credId,
            BusinessDetails.key,
            businessDetails.copy(previouslyRegistered = Some(data), hasChanged = true)
          )
        }
      }
    }
  }  flatMap(_.sequence)
}