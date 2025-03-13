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

import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, RiskAssessmentPolicy, RiskAssessmentTypes}
import models.businessmatching.BusinessMatching

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DocumentRiskAssessmentService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit
  val ec: ExecutionContext
) {

  def getRiskAssessmentPolicy(credId: String): Future[Option[RiskAssessmentPolicy]] =
    dataCacheConnector.fetch[BusinessActivities](credId, BusinessActivities.key).map(_.flatMap(_.riskAssessmentPolicy))

  def updateRiskAssessmentType(credId: String, data: RiskAssessmentTypes): Future[Option[BusinessMatching]] =
    dataCacheConnector.fetchAll(credId).map { optCacheMap =>
      val businessMatchingAndActivities: Option[(BusinessMatching, BusinessActivities)] = for {
        cacheMap           <- optCacheMap
        businessMatching   <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
        businessActivities <- cacheMap.getEntry[BusinessActivities](BusinessActivities.key)
      } yield (businessMatching, businessActivities)

      businessMatchingAndActivities
        .map(bmba =>
          dataCacheConnector.save[BusinessActivities](credId, BusinessActivities.key, bmba._2.riskAssessmentTypes(data))
        )
        .flatMap(_ => businessMatchingAndActivities.map(_._1))
    }
}
