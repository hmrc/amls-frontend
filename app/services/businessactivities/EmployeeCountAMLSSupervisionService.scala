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
import models.businessactivities.{BusinessActivities, EmployeeCountAMLSSupervision, HowManyEmployees}
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployeeCountAMLSSupervisionService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit
  ec: ExecutionContext
) {

  def getEmployeeCountAMLSSupervision(credId: String): Future[Option[String]] =
    dataCacheConnector
      .fetch[BusinessActivities](credId, BusinessActivities.key)
      .map(
        _.map(_.howManyEmployees)
          .flatMap(_.flatMap(_.employeeCountAMLSSupervision))
      )

  def updateHowManyEmployees(credId: String, data: EmployeeCountAMLSSupervision): Future[Option[Cache]] = {
    dataCacheConnector.fetch[BusinessActivities](credId, BusinessActivities.key) map { baOpt =>
      baOpt flatMap { ba =>
        ba.howManyEmployees map { employees =>
          dataCacheConnector.save[BusinessActivities](
            credId,
            BusinessActivities.key,
            ba.howManyEmployees(HowManyEmployees(employees.employeeCount, Some(data.employeeCountAMLSSupervision)))
          )
        }
      }
    }
  } flatMap (_.sequence)
}
