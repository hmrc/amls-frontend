/*
 * Copyright 2017 HM Revenue & Customs
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

package services.businessmatching

import cats.implicits._
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.BusinessMatching
import models.status.{NotCompleted, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.{DependencyMocks, FutureAssertions}

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessMatchingServiceSpec extends PlaySpec
  with MockitoSugar
  with ScalaFutures
  with FutureAssertions
  with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks {
    val service = new BusinessMatchingService(mockStatusService, mockCacheConnector)

    val primaryModel = businessMatchingGen.sample.get
    val variationModel = businessMatchingGen.sample.get

    mockCacheFetch(Some(primaryModel), Some(BusinessMatching.key))
    mockCacheFetch(Some(variationModel), Some(BusinessMatching.variationKey))
    mockCacheSave[BusinessMatching]

  }

  "getModel" when {
    "called" must {
      "return the primary model" when {
        "in a pre-application status" in new Fixture {
          mockApplicationStatus(NotCompleted)
          service.getModel returnsSome primaryModel
        }

        "the variation model is empty and status is post-preapp" in new Fixture {
          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheFetch(Some(BusinessMatching()), Some(BusinessMatching.variationKey))

          service.getModel returnsSome primaryModel
        }
      }

      "return the variation model" when {
        "in a amendment or variation status" in new Fixture {
          mockApplicationStatus(SubmissionDecisionApproved)
          service.getModel returnsSome variationModel
        }
      }

      "not query for the original model when the variation model exists" in new Fixture {
        mockApplicationStatus(SubmissionReadyForReview)
        service.getModel returnsSome variationModel
        verify(mockCacheConnector, never).fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      }
    }
  }

  "updateModel" when {
    "called" must {
      "update the original model" when {
        "in pre-application status" in new Fixture {
          mockApplicationStatus(NotCompleted)
          mockCacheSave(primaryModel)

          service.updateModel(primaryModel) returnsSome mockCacheMap
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
        }
      }

      "update the variation model" when {
        "not in pre-application status" in new Fixture {
          mockApplicationStatus(SubmissionReadyForReview)
          mockCacheSave(primaryModel)

          whenReady(service.updateModel(primaryModel).value) { _ =>
            verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), any())(any(), any(), any())
          }
        }
      }
    }
  }

  "commitVariationData" when {
    "called" must {
      "simply return the cachemap when in pre-application status" in new Fixture {
        mockApplicationStatus(SubmissionReady)
        service.commitVariationData returnsSome mockCacheMap
      }

      "copy the variation data over the primary data when not in pre-application status" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(primaryModel.some, BusinessMatching.key)
        mockCacheGetEntry(variationModel.some, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { _ =>
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.key), eqTo(variationModel.copy(hasChanged = true)))(any(), any(), any())
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), eqTo(BusinessMatching()))(any(), any(), any())
        }
      }

      "copy the variation data over the primary data, setting hasChanged to false when the models are the same" in new Fixture {
        val newModel = businessMatchingGen.sample

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(newModel, BusinessMatching.key)
        mockCacheGetEntry(newModel, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { _ =>
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.key), eqTo(newModel.copy(hasChanged = false)))(any(), any(), any())
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), eqTo(BusinessMatching()))(any(), any(), any())
        }
      }

      "return None if the variation data is not available" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(primaryModel.some, BusinessMatching.key)
        mockCacheGetEntry(None, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { result =>
          verify(mockCacheConnector, never).save[BusinessMatching](any(), any())(any(), any(), any())
          result mustBe None
        }
      }

      "return None if the variation data is empty" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(primaryModel.some, BusinessMatching.key)
        mockCacheGetEntry(BusinessMatching().some, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { result =>
          verify(mockCacheConnector, never).save[BusinessMatching](any(), any())(any(), any(), any())
          result mustBe None
        }
      }

    }
  }

  "clear" when {
    "called" must {
      "reset the variation model back to nothing" in new Fixture {
        whenReady(service.clearVariation.value) { _ =>
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), eqTo(BusinessMatching()))(any(), any(), any())
        }
      }
    }
  }
}
