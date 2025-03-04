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
import models.businessactivities.{BusinessActivities, EmployeeCount, HowManyEmployees}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class HowManyEmployeesServiceSpec extends AmlsSpec with BeforeAndAfterEach with IntegrationPatience {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap       = mock[Cache]

  val service = new HowManyEmployeesService(mockCacheConnector)

  val credId = "123456"

  override protected def beforeEach(): Unit = reset(mockCacheConnector, mockCacheMap)

  "HowManyEmployeesService" when {

    "getEmployeeCount is called" must {

      "return the count when it is present in Business Activities" in {

        val count = "92"

        when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
          .thenReturn(Future.successful(Some(BusinessActivities().howManyEmployees(HowManyEmployees(Some(count))))))

        service.getEmployeeCount(credId).futureValue mustBe Some(count)
      }

      "return None" when {

        "employee count is not set in Business Activities" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(Some(BusinessActivities().howManyEmployees(HowManyEmployees()))))

          service.getEmployeeCount(credId).futureValue mustBe None
        }

        "how many employees is not set in Business Activities" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(Some(BusinessActivities())))

          service.getEmployeeCount(credId).futureValue mustBe None
        }

        "business activities is not set in cache" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(None))

          service.getEmployeeCount(credId).futureValue mustBe None
        }
      }
    }

    "updateEmployeeCount is called" must {

      "update model correctly" in {

        val ba   = BusinessActivities().howManyEmployees(HowManyEmployees(employeeCountAMLSSupervision = Some("54")))
        val data = EmployeeCount("86")

        when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
          .thenReturn(Future.successful(Some(ba)))

        when(
          mockCacheConnector.save[BusinessActivities](
            eqTo(credId),
            eqTo(BusinessActivities.key),
            eqTo(ba.howManyEmployees(HowManyEmployees(Some(data.employeeCount), Some("54"))))
          )(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        service.updateEmployeeCount(credId, data).futureValue mustBe Some(mockCacheMap)

        verify(mockCacheConnector).save[BusinessActivities](
          eqTo(credId),
          eqTo(BusinessActivities.key),
          eqTo(ba.howManyEmployees(HowManyEmployees(Some(data.employeeCount), Some("54"))))
        )(any())
      }

      "not update model when business activities is not set" in {

        when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
          .thenReturn(Future.successful(None))

        service.updateEmployeeCount(credId, EmployeeCount("17")).futureValue mustBe None

        verify(mockCacheConnector, times(0))
          .save[BusinessActivities](any(), any(), any())(any())
      }
    }
  }
}
