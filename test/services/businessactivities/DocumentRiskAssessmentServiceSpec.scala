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
import models.businessactivities._
import models.businessmatching.BusinessMatching
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class DocumentRiskAssessmentServiceSpec extends AmlsSpec with BeforeAndAfterEach {

  val mockCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockCacheMap: Cache                    = mock[Cache]

  val service = new DocumentRiskAssessmentService(mockCacheConnector)

  val credId = "123456"

  override def beforeEach(): Unit = reset(mockCacheConnector)

  "DocumentRiskAssessmentService" when {

    "getRiskAssessmentPolicy is called" must {

      "return Risk Assessment Policy" when {

        "it is present in the Business Activities model" in {

          val riskAssessmentPolicy = RiskAssessmentPolicy(
            RiskAssessmentHasPolicy(true),
            RiskAssessmentTypes(Set(PaperBased, Digital))
          )

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(Some(BusinessActivities().riskAssessmentPolicy(riskAssessmentPolicy))))

          service.getRiskAssessmentPolicy(credId).futureValue mustBe Some(riskAssessmentPolicy)
        }
      }

      "return None" when {

        "Risk Assessment Policy is not present in Business Activities model" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(Some(BusinessActivities())))

          service.getRiskAssessmentPolicy(credId).futureValue mustBe None
        }

        "Business Activities model can't be retrieved" in {

          when(mockCacheConnector.fetch[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key))(any()))
            .thenReturn(Future.successful(None))

          service.getRiskAssessmentPolicy(credId).futureValue mustBe None
        }
      }
    }

    "updateRiskAssessmentType is called" must {

      val bm          = BusinessMatching()
      val ba          = BusinessActivities()
      val updateModel = RiskAssessmentTypes(RiskAssessmentType.all.toSet)

      "save Business Activities model and return Business Matching object" in {

        when(mockCacheConnector.fetchAll(eqTo(credId)))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(bm))

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(ba))

        when(
          mockCacheConnector.save[BusinessActivities](
            eqTo(credId),
            eqTo(BusinessActivities.key),
            eqTo(ba.riskAssessmentTypes(updateModel))
          )(any())
        ).thenReturn(Future.successful(mockCacheMap))

        service.updateRiskAssessmentType(credId, updateModel).futureValue mustBe Some(bm)

        verify(mockCacheConnector).save[BusinessActivities](
          eqTo(credId),
          eqTo(BusinessActivities.key),
          eqTo(ba.riskAssessmentTypes(updateModel))
        )(any())
      }

      "not save Business Activities model" when {

        "Business Activities is not present in cache" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(bm))

          when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(None)

          service.updateRiskAssessmentType(credId, updateModel).futureValue mustBe None

          verify(mockCacheConnector, times(0))
            .save[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key), any())(any())
        }

        "Business Matching is not present in cache" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(None)

          when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(Some(ba))

          service.updateRiskAssessmentType(credId, updateModel).futureValue mustBe None

          verify(mockCacheConnector, times(0))
            .save[BusinessActivities](eqTo(credId), eqTo(BusinessActivities.key), any())(any())
        }
      }
    }
  }
}
