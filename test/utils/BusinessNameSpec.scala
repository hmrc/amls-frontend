/*
 * Copyright 2019 HM Revenue & Customs
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

package utils

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.ReadStatusResponse
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.registrationdetails.RegistrationDetails
import models.status.SubmissionReady
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessNameSpec extends AmlsSpec with ScalaFutures {

  trait Fixture {
    implicit val amlsConnector = mock[AmlsConnector]
    implicit val cacheConnector = mock[DataCacheConnector]
    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]
    implicit val statusResponse = mock[ReadStatusResponse]
    implicit val statusService = mock[StatusService]

    val safeId = "X87FUDIKJJKJH87364"
  }

  "The BusinessName helper utility" must {
    "get the business name from amls" in new Fixture {
      when {
        amlsConnector.registrationDetails(eqTo(safeId))(any(), any(), any())
      } thenReturn Future.successful(RegistrationDetails("Test Business", isIndividual = false))

      whenReady(BusinessName.getName(safeId.some).value) { result =>
        result mustBe "Test Business".some
      }
    }

    "get the name from the data cache" when {
      "the safeId is not available" in new Fixture {

        val reviewDetails = mock[ReviewDetails]
        when(reviewDetails.businessName) thenReturn "Test Business from the cache"

        when {
          cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
        } thenReturn Future.successful(BusinessMatching(reviewDetails = reviewDetails.some).some)

        whenReady(BusinessName.getName(None).value) { result =>
          result mustBe "Test Business from the cache".some
        }
      }
    }

    "get the name from amls" when {
      "the safeId comes from API9" in new Fixture {

        when(statusResponse.safeId) thenReturn Some(safeId)

        when {
          statusService.getDetailedStatus(any(), any(), any())
        } thenReturn Future.successful((SubmissionReady, Some(statusResponse)))

        when {
          amlsConnector.registrationDetails(eqTo(safeId))(any(), any(), any())
        } thenReturn Future.successful(RegistrationDetails("Test Business", isIndividual = false))

        whenReady(BusinessName.getBusinessNameFromAmls().value) { result =>
          result mustBe "Test Business".some
        }
      }
    }
  }
}
