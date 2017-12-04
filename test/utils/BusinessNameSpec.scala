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

package utils

import connectors.{AmlsConnector, DataCacheConnector}
import models.registrationdetails.RegistrationDetails
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import cats.implicits._
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.frontend.auth.AuthContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class BusinessNameSpec extends PlaySpec with MustMatchers with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture {
    implicit val amlsConnector = mock[AmlsConnector]
    implicit val cacheConnector = mock[DataCacheConnector]
    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

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
  }
}
