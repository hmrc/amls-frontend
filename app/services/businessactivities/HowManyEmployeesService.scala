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
import models.businessactivities.{BusinessActivities, EmployeeCount, HowManyEmployees}
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HowManyEmployeesService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit ec: ExecutionContext) {

  def getEmployeeCount(credId: String): Future[Option[String]] =
    dataCacheConnector
      .fetch[BusinessActivities](credId, BusinessActivities.key)
      .map(optBusinessActivities =>
        optBusinessActivities
          .flatMap(ba => ba.howManyEmployees.flatMap(_.employeeCount))
      )

  def updateEmployeeCount(credId: String, data: EmployeeCount): Future[Option[Cache]] = {
    dataCacheConnector.fetch[BusinessActivities](credId, BusinessActivities.key) map {
      _ map { ba =>
        dataCacheConnector.save[BusinessActivities](
          credId,
          BusinessActivities.key,
          ba.howManyEmployees(updateData(ba.howManyEmployees, data))
        )
      }
    }
  } flatMap (_.sequence)

  private def updateData(employeesOpt: Option[HowManyEmployees], data: EmployeeCount): HowManyEmployees =
    employeesOpt.fold[HowManyEmployees](HowManyEmployees(employeeCount = Some(data.employeeCount))) { employees =>
      employees.copy(employeeCount = Some(data.employeeCount))
    }
}
