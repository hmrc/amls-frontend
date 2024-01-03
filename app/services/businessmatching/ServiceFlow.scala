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

import cats.implicits._
import connectors.DataCacheConnector
import javax.inject.Inject
import models.businessmatching.BusinessActivity
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}

case class NextService(url: String, activity: BusinessActivity)

class ServiceFlow @Inject()(businessMatchingService: BusinessMatchingService, cacheConnector: DataCacheConnector) {

  def isNewActivity(cacheId: String, activity: BusinessActivity)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    businessMatchingService.getAdditionalBusinessActivities(cacheId)
      .map {
        _.contains(activity)
      } getOrElse false
}
