/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import scala.concurrent.{Await, Future}
import generators.AmlsReferenceNumberGenerator
import generators.submission.SubscriptionResponseGenerator
import models.ResponseType.SubscriptionResponseType
import models.{FeeResponse, ResponseType}
import org.joda.time.DateTime
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import services.{AuthEnrolmentsService, FeeResponseService}
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class FeeHelperSpec extends PlaySpec with MockitoSugar
  with ScalaFutures
  with GuiceOneAppPerSuite
  with AmlsReferenceNumberGenerator
  with SubscriptionResponseGenerator{

  "FeeHelper" when {
    implicit val hc = HeaderCarrier()

    val feeHelper = new FeeHelper(
      feeResponseService = mock[FeeResponseService],
      enrolmentService = mock[AuthEnrolmentsService]
    )

    def feeResponse(responseType: ResponseType) = FeeResponse(
      responseType = responseType,
      amlsReferenceNumber = amlsRegistrationNumber,
      registrationFee = 100,
      fpFee = None,
      approvalCheckFee = None,
      premiseFee = 0,
      totalFees = 200,
      paymentReference = Some(paymentReferenceNumber),
      difference = Some(115),
      createdAt = DateTime.now
    )

    val response = feeResponse(SubscriptionResponseType)

    "the user has fees" must {
      "fetch fees" in  {
        when(feeHelper.enrolmentService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

        when {
          feeHelper.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber), any[(String, String)]())(any(), any())
        } thenReturn Future.successful(Some(response))

        val result = Await.result(
          feeHelper.retrieveFeeResponse(Some(amlsRegistrationNumber), ("foo", "bar"), None, "feeHelper"), 5 seconds
        )
        result.isDefined mustBe true
      }
    }

    "the user has no fees" must {
      "fetch fees" in  {
        when(feeHelper.enrolmentService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

        when {
          feeHelper.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber), any[(String, String)]())(any(), any())
        } thenReturn Future.successful(None)

        val result = Await.result(
          feeHelper.retrieveFeeResponse(Some(amlsRegistrationNumber), ("foo", "bar"), None, "feeHelper"), 5 seconds
        )
        result.isDefined mustBe false
      }
    }
  }
}



