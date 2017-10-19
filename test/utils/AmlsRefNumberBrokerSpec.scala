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

import generators.PaymentGenerator
import models.confirmation.{BreakdownRow, Currency}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => eqTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import services.{AuthEnrolmentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class AmlsRefNumberBrokerSpec extends PlaySpec with MustMatchers with MockitoSugar with PaymentGenerator with ScalaFutures {

  trait Fixture {

    implicit val hc = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

    val broker = new AmlsRefNumberBroker {
      override private[utils] val submissionResponseService = mock[SubmissionResponseService]
      override private[utils] val authEnrolmentsService = mock[AuthEnrolmentsService]
      override private[utils] val statusService = mock[StatusService]
    }

  }

  "The AMLS Reference number broker" must {
    "return the reference number from the submission response" in new Fixture {
      val status = SubmissionDecisionApproved

      when {
        broker.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(status)

      when {
        broker.submissionResponseService.getSubmissionData(eqTo(status))(any(), any(), any())
      } thenReturn Future.successful(Some((paymentRefGen.sample, Currency.fromInt(0), Seq.empty[BreakdownRow], Left(amlsRegistrationNumber))))

      whenReady(broker.get.value) { r => r mustBe Some(amlsRegistrationNumber) }

      verify(broker.authEnrolmentsService, never).amlsRegistrationNumber(any(), any(), any())
    }

    "return the reference number from the enrolments service when the submission response is unfavourable" in new Fixture {
      val status = SubmissionDecisionApproved

      when {
        broker.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(status)

      when {
        broker.submissionResponseService.getSubmissionData(eqTo(status))(any(), any(), any())
      } thenReturn Future.successful(Some((paymentRefGen.sample, Currency.fromInt(0), Seq.empty[BreakdownRow], Right(Some(Currency.fromInt(0))))))

      when {
        broker.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some(amlsRegistrationNumber))

      whenReady(broker.get.value) { r => r mustBe Some(amlsRegistrationNumber) }



    }
  }

}
