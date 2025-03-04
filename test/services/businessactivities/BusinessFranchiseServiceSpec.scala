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
import models.businessactivities.{BusinessActivities, BusinessFranchiseYes}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class BusinessFranchiseServiceSpec extends AmlsSpec with BeforeAndAfterEach {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap       = mock[Cache]

  val service = new BusinessFranchiseService(mockCacheConnector)

  val credId = "123456"

  override def beforeEach(): Unit = reset(mockCacheConnector, mockCacheMap)

  "BusinessFranchiseService" when {

    "getBusinessFranchise is called" must {

      "return Business Franchise model from Business Activities when it is present in the cache" in {

        val businessFranchise = BusinessFranchiseYes("Name of Franchise")

        when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
          .thenReturn(Future.successful(Some(BusinessActivities().businessFranchise(businessFranchise))))

        service.getBusinessFranchise(credId).futureValue mustBe Some(businessFranchise)
      }

      "return None" when {

        "Business Franchise is not set" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(Some(BusinessActivities())))

          service.getBusinessFranchise(credId).futureValue mustBe None
        }

        "Business Activities is not preset" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(None))

          service.getBusinessFranchise(credId).futureValue mustBe None
        }
      }
    }

    "updateBusinessFranchise is called" must {

      "fetch Business Activities and save business franchise with updated model" when {

        "Business Activities is successfully retrieved from cache" in {

          val franchise       = BusinessFranchiseYes("name")
          val modelWithUpdate = BusinessActivities().businessFranchise(franchise)

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(Some(BusinessActivities())))

          when(
            mockCacheConnector
              .save[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key), eqTo(modelWithUpdate))(any())
          ) thenReturn Future.successful(mockCacheMap)

          service.updateBusinessFranchise(credId, franchise).futureValue mustBe mockCacheMap

          verify(mockCacheConnector).fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any())
          verify(mockCacheConnector)
            .save[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key), eqTo(modelWithUpdate))(any())
        }
      }
    }
  }
}
