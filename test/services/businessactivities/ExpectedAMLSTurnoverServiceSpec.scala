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
import models.businessactivities.BusinessActivities
import models.businessactivities.ExpectedAMLSTurnover.First
import models.businessmatching.BusinessMatching
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class ExpectedAMLSTurnoverServiceSpec extends AmlsSpec with BeforeAndAfterEach with IntegrationPatience {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap       = mock[Cache]

  val service = new ExpectedAMLSTurnoverService(mockCacheConnector)

  val credId = "123456"

  override def beforeEach(): Unit = reset(mockCacheConnector, mockCacheMap)

  "ExpectedAMLSTurnoverService" when {

    "getBusinessMatchingExpectedTurnover is called" must {

      "return business matching and expected turnover" when {

        "matching and activities are present in the cache and turnover is present in activities" in {

          val bm = BusinessMatching()
          val ba = BusinessActivities().expectedAMLSTurnover(First)

          when(mockCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(bm))

          when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(Some(ba))

          service.getBusinessMatchingExpectedTurnover(credId).futureValue mustBe Some((bm, Some(First)))
        }
      }

      "return only business matching" when {

        "matching and activities are present in the cache but turnover is not in activities" in {

          val bm = BusinessMatching()
          val ba = BusinessActivities()

          when(mockCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(bm))

          when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(Some(ba))

          service.getBusinessMatchingExpectedTurnover(credId).futureValue mustBe Some((bm, None))
        }

        "matching is present in the cache but activities is not present" in {

          val bm = BusinessMatching()

          when(mockCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(bm))

          when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(None)

          service.getBusinessMatchingExpectedTurnover(credId).futureValue mustBe Some((bm, None))
        }
      }

      "return None" when {

        "neither matching or activities are present in the cache" in {

          when(mockCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(None)

          when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(None)

          service.getBusinessMatchingExpectedTurnover(credId).futureValue mustBe None
        }

        "cache cannot be retrieved" in {

          when(mockCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(None))

          service.getBusinessMatchingExpectedTurnover(credId).futureValue mustBe None
        }
      }
    }

    "getBusinessMatching is called" must {

      "return business matching if it is present in the cache" in {

        val bm = BusinessMatching().copy(hasAccepted = true)

        when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
          .thenReturn(Future.successful(Some(bm)))

        service.getBusinessMatching(credId).futureValue mustBe Some(bm)
      }

      "return None if business matching is not present in the cache" in {

        when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
          .thenReturn(Future.successful(None))

        service.getBusinessMatching(credId).futureValue mustBe None
      }
    }

    "updateBusinessActivities is called" must {

      "successfully save updated model to cache" in {

        val ba = BusinessActivities()

        when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
          .thenReturn(Future.successful(Some(ba)))

        when(
          mockCacheConnector.save[BusinessActivities](
            eqTo(credId),
            eqTo(BusinessActivities.key),
            eqTo(ba.expectedAMLSTurnover(First))
          )(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        service.updateBusinessActivities(credId, First).futureValue mustBe Some(mockCacheMap)

        verify(mockCacheConnector).save[BusinessActivities](
          eqTo(credId),
          eqTo(BusinessActivities.key),
          eqTo(ba.expectedAMLSTurnover(First))
        )(any())
      }

      "not save model if business activities cannot be retrieved from cache" in {

        when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
          .thenReturn(Future.successful(None))

        service.updateBusinessActivities(credId, First).foreach {
          _ mustBe None
        }
        verify(mockCacheConnector, times(0))
          .save[BusinessActivities](any(), any(), any())(any())
      }
    }
  }
}
