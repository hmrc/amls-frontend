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
import models.businessactivities.{BusinessActivities, EmployeeCountAMLSSupervision, HowManyEmployees}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class EmployeeCountAMLSSupervisionServiceSpec extends AmlsSpec with BeforeAndAfterEach with IntegrationPatience {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap       = mock[Cache]

  val service = new EmployeeCountAMLSSupervisionService(mockCacheConnector)

  val credId = "123456"

  override def beforeEach(): Unit = reset(mockCacheConnector, mockCacheMap)

  "EmployeeCountAMLSSupervisionService" when {

    "getEmployeeCountAMLSSupervision is called" must {

      "return the correct employee count" when {

        "it is present in the cache" in {
          val count = "38"

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(
              Future.successful(
                Some(
                  BusinessActivities(howManyEmployees = Some(HowManyEmployees(Some("648"), Some(count))))
                )
              )
            )

          service.getEmployeeCountAMLSSupervision(credId).futureValue mustBe Some(count)
        }
      }

      "return None" when {

        "employeeCountAMLSSupervision is not set in the cache" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(
              Future.successful(
                Some(
                  BusinessActivities(howManyEmployees = Some(HowManyEmployees(Some("648"), None)))
                )
              )
            )

          service.getEmployeeCountAMLSSupervision(credId).futureValue mustBe None
        }

        "HowManyEmployees is not set in the cache" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(Some(BusinessActivities())))

          service.getEmployeeCountAMLSSupervision(credId).futureValue mustBe None
        }

        "BusinessActivities is not set in the cache" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(None))

          service.getEmployeeCountAMLSSupervision(credId).futureValue mustBe None
        }
      }
    }

    "updateHowManyEmployees is called" must {

      "update and save the model" in {

        val count = "82"

        val bm = BusinessActivities().howManyEmployees(HowManyEmployees(Some("815"), None))

        when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
          .thenReturn(
            Future.successful(Some(bm))
          )

        when(
          mockCacheConnector.save[BusinessActivities](
            eqTo(credId),
            eqTo(BusinessActivities.key),
            any()
          )(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        service.updateHowManyEmployees(credId, EmployeeCountAMLSSupervision(count)).futureValue mustBe Some(
          mockCacheMap
        )

        verify(mockCacheConnector).save[BusinessActivities](
          eqTo(credId),
          eqTo(BusinessActivities.key),
          eqTo(bm.howManyEmployees(HowManyEmployees(Some("815"), Some(count))))
        )(any())
      }

      "fail to update or save the model" when {

        "Business Activities is not present in cache" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(None))

          service.updateHowManyEmployees(credId, EmployeeCountAMLSSupervision("15")).futureValue mustBe None

          verify(mockCacheConnector, times(0))
            .save[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key), any())(any())
        }
      }
    }
  }
}
