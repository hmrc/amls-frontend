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
import models.asp.{Asp, Service, ServicesOfBusiness}
import models.businessdetails.{ActivityStartDate, BusinessDetails}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import services.cache.Cache
import utils.AmlsSpec

import java.time.LocalDate
import scala.concurrent.Future

class ServicesOfBusinessDateOfChangeServiceSpec extends AmlsSpec with BeforeAndAfterEach {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap       = mock[Cache]

  val service = new ServicesOfBusinessDateOfChangeService(mockCacheConnector)

  val cacheId = "123456"

  val date              = LocalDate.now()
  val activityStartDate = ActivityStartDate(date)

  val asp = Asp().services(ServicesOfBusiness(Service.all.toSet))

  override def beforeEach(): Unit = reset(mockCacheConnector, mockCacheMap)

  "ServicesOfBusinessDateOfChangeService" when {

    "getModelWithDate is called" must {

      "return model and start date" when {

        "business details contains start date and model is present in cache" in {

          when(mockCacheConnector.fetchAll(eqTo(cacheId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(Some(BusinessDetails().activityStartDate(activityStartDate)))

          when(mockCacheMap.getEntry[Asp](eqTo(Asp.key))(any()))
            .thenReturn(Some(asp))

          service.getModelWithDate(cacheId).futureValue mustBe ((asp, Some(activityStartDate)))
        }
      }

      "return just a model" when {

        "model is present in cache but business details do not contain start date" in {

          when(mockCacheConnector.fetchAll(eqTo(cacheId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(Some(BusinessDetails()))

          when(mockCacheMap.getEntry[Asp](eqTo(Asp.key))(any()))
            .thenReturn(Some(asp))

          service.getModelWithDate(cacheId).futureValue mustBe ((asp, None))
        }
      }

      "return an empty model" when {

        "model is not present and business details do not contain start date" in {

          when(mockCacheConnector.fetchAll(eqTo(cacheId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(None)

          when(mockCacheMap.getEntry[Asp](eqTo(Asp.key))(any()))
            .thenReturn(None)

          service.getModelWithDate(cacheId).futureValue mustBe ((Asp(), None))
        }
      }
    }

    "updateAsp is called" must {

      "update Asp with the date of change" in {

        val updatedAsp = asp.services(ServicesOfBusiness(Service.all.toSet, Some(DateOfChange(date))))

        when(mockCacheConnector.save[Asp](eqTo(cacheId), eqTo(Asp.key), eqTo(updatedAsp))(any()))
          .thenReturn(Future.successful(mockCacheMap))

        when(mockCacheMap.getEntry[Asp](eqTo(Asp.key))(any()))
          .thenReturn(Some(updatedAsp))

        service.updateAsp(asp, DateOfChange(date), cacheId).futureValue mustBe Some(updatedAsp)
      }

      "not save Asp model" when {

        "services in Asp model is empty" in {

          val emptyAsp = Asp()

          service.updateAsp(emptyAsp, DateOfChange(date), cacheId).futureValue mustBe Some(emptyAsp)

          verifyNoInteractions(mockCacheConnector, mockCacheMap)
        }
      }
    }
  }
}
