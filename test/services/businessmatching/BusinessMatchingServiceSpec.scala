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

import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.BusinessMatching
import models.status.{NotCompleted, SubmissionDecisionApproved, SubmissionReadyForReview}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito.{verify, never}
import org.mockito.Matchers.{eq => eqTo, any}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{DependencyMocks, FutureAssertions}

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessMatchingServiceSpec extends PlaySpec
  with MockitoSugar
  with ScalaFutures
  with FutureAssertions
  with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks {
    val service = new BusinessMatchingService(mockStatusService, mockCacheConnector)

    val primaryModel = businessMatchingGen.sample
    val variationModel = businessMatchingGen.sample

    mockCacheFetch(primaryModel, Some(BusinessMatching.key))
    mockCacheFetch(variationModel, Some(BusinessMatching.variationKey))
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

}
