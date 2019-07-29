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

package services

import cats.implicits._
import connectors.FeeConnector
import models.FeeResponse
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.NotFoundException
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FeeResponseServiceSpec extends AmlsSpec with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>

    val mockFeeConnector = mock[FeeConnector]

    val testFeeResponseService = new FeeResponseService(mockFeeConnector)

    val testAmlsReference: String = "XAML0000000001"

    val testFeeResponseSubscription = FeeResponse(
      SubscriptionResponseType,
      "XAML0000000001",
      BigDecimal(100),
      Some(BigDecimal(100)),
      None,
      BigDecimal(100),
      BigDecimal(100),
      None,
      None,
      new DateTime(2018, 1, 1, 0, 0)
    )

    val testFeeResponseAmendVariation = FeeResponse(
      AmendOrVariationResponseType,
      "XAML0000000001",
      BigDecimal(100),
      Some(BigDecimal(100)),
      None,
      BigDecimal(100),
      BigDecimal(100),
      None,
      Some(BigDecimal(100)),
      new DateTime(2018, 1, 1, 0, 0)
    )

    def setupMockFeeConnector(amlsReference: String, feeResponse: Option[FeeResponse] = None, exception: Option[Throwable] = None) =
      (feeResponse, exception) match {
      case (Some(fees), _) => when(mockFeeConnector.feeResponse(eqTo(amlsReference))(any(), any(), any(), any()))
        .thenReturn(Future.successful(fees))
      case (_ , Some(e)) => when(mockFeeConnector.feeResponse(any())(any(), any(), any(), any())).
        thenReturn(Future.failed(e))
    }
  }

  "FeeResponseService" must {
    "have getFeeResponse method which" when {
      "called with amls reference number will return fee response for Subscription" in new Fixture {

        setupMockFeeConnector(testAmlsReference, testFeeResponseSubscription.some)

        whenReady(testFeeResponseService.getFeeResponse(testAmlsReference)) { result =>
          result mustBe testFeeResponseSubscription.some
        }
      }

      "called with amls reference number will return fee response for Amendment or Variation" in new Fixture {

        setupMockFeeConnector(testAmlsReference, testFeeResponseAmendVariation.some)

        whenReady(testFeeResponseService.getFeeResponse(testAmlsReference)) { result =>
          result mustBe testFeeResponseAmendVariation.some
        }
      }

      "called with amls reference number will return none if no fees retuned from connector" in new Fixture {

        setupMockFeeConnector(testAmlsReference, None, new NotFoundException("not found").some)

        when(mockFeeConnector.feeResponse(any())(any(), any(), any(), any())).thenReturn(Future.failed(new NotFoundException("not found")))

        whenReady(testFeeResponseService.getFeeResponse(testAmlsReference)) { result =>
          result mustBe None
        }
      }
    }
  }
}
