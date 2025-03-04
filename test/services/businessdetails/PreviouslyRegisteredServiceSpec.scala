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

package services.businessdetails

import connectors.DataCacheConnector
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class PreviouslyRegisteredServiceSpec extends AmlsSpec with BeforeAndAfterEach {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap       = mock[Cache]

  val service = new PreviouslyRegisteredService(mockCacheConnector)

  val credId = "123456"

  override def beforeEach(): Unit = reset(mockCacheConnector, mockCacheMap)

  "PreviouslyRegisteredService" when {

    "getPreviouslyRegistered is called" must {

      "return the model if present" in {

        when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
          .thenReturn(Future.successful(Some(BusinessDetails().previouslyRegistered(PreviouslyRegisteredNo))))

        service.getPreviouslyRegistered(credId).futureValue mustBe Some(PreviouslyRegisteredNo)
      }

      "return none" when {

        "previously registered is not present" in {

          when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
            .thenReturn(Future.successful(Some(BusinessDetails())))

          service.getPreviouslyRegistered(credId).futureValue mustBe None
        }

        "business details is not present" in {

          when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
            .thenReturn(Future.successful(None))

          service.getPreviouslyRegistered(credId).futureValue mustBe None
        }
      }
    }

    "updatePreviouslyRegistered is called" must {

      "update and save the model correctly" when {

        "business details are returned from the cache" in {

          val bd = BusinessDetails()

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(Some(bd))

          when(
            mockCacheConnector.save[BusinessDetails](
              eqTo(credId),
              eqTo(BusinessDetails.key),
              eqTo(bd.copy(previouslyRegistered = Some(PreviouslyRegisteredYes(None)), hasChanged = true))
            )(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          service.updatePreviouslyRegistered(credId, PreviouslyRegisteredYes(None)).futureValue mustBe Some(
            mockCacheMap
          )

          verify(mockCacheConnector).save[BusinessDetails](
            eqTo(credId),
            eqTo(BusinessDetails.key),
            eqTo(bd.copy(previouslyRegistered = Some(PreviouslyRegisteredYes(None)), hasChanged = true))
          )(any())
        }

        "business details is not yet saved in the cache" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(None)

          when(
            mockCacheConnector.save[BusinessDetails](
              eqTo(credId),
              eqTo(BusinessDetails.key),
              eqTo(BusinessDetails().copy(previouslyRegistered = Some(PreviouslyRegisteredNo), hasChanged = true))
            )(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          service.updatePreviouslyRegistered(credId, PreviouslyRegisteredNo).futureValue mustBe Some(mockCacheMap)

          verify(mockCacheConnector).save[BusinessDetails](
            eqTo(credId),
            eqTo(BusinessDetails.key),
            eqTo(BusinessDetails().copy(previouslyRegistered = Some(PreviouslyRegisteredNo), hasChanged = true))
          )(any())
        }
      }

      "not update or save the model" when {

        "cache is empty" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(None))

          service.updatePreviouslyRegistered(credId, PreviouslyRegisteredNo).futureValue mustBe None

          verify(mockCacheConnector).save[BusinessDetails](
            eqTo(credId),
            eqTo(BusinessDetails.key),
            eqTo(None)
          )(any())
        }

        "save throws an exception" in {

          val bd = BusinessDetails()

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(Some(bd))

          when(
            mockCacheConnector.save[BusinessDetails](
              eqTo(credId),
              eqTo(BusinessDetails.key),
              eqTo(bd.copy(previouslyRegistered = Some(PreviouslyRegisteredNo), hasChanged = true))
            )(any())
          )
            .thenReturn(Future.failed(new Exception("Something went wrong")))

          service.updatePreviouslyRegistered(credId, PreviouslyRegisteredNo).futureValue mustBe None
        }
      }
    }
  }
}
