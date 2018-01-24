/*
 * Copyright 2018 HM Revenue & Customs
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
import models.confirmation.{BreakdownRow, Currency, SubmissionData}
import models.status.SubmissionDecisionApproved
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import services.{AuthEnrolmentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsRefNumberBrokerSpec extends PlaySpec with GenericTestHelper with MustMatchers with MockitoSugar with PaymentGenerator with ScalaFutures {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[SubmissionResponseService].to(mock[SubmissionResponseService]))
    .build()

  trait Fixture {

    implicit val hc = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

    val broker = new AmlsRefNumberBroker (
      mock[StatusService],
      mock[AuthEnrolmentsService]
    )

  }

  "The AMLS Reference number broker" must {
    "return the reference number from the submission response" in new Fixture {
      val status = SubmissionDecisionApproved

      when {
        broker.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(status)

      when {
        broker.submissionResponseService.getSubmissionData(eqTo(status), any())(any(), any(), any())
      } thenReturn Future.successful(Some(SubmissionData(paymentRefGen.sample, Currency.fromInt(0), Seq.empty[BreakdownRow], Some(amlsRegistrationNumber), None)))

      whenReady(broker.get.value) { r => r mustBe Some(amlsRegistrationNumber) }

      verify(broker.authEnrolmentsService, never).amlsRegistrationNumber(any(), any(), any())
    }

    "return the reference number from the enrolments service when the submission response is unfavourable" in new Fixture {
      val status = SubmissionDecisionApproved

      when {
        broker.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(status)

      when {
        broker.submissionResponseService.getSubmissionData(eqTo(status),any())(any(), any(), any())
      } thenReturn Future.successful(Some(SubmissionData(paymentRefGen.sample, Currency.fromInt(0), Seq.empty[BreakdownRow], None, Some(Currency.fromInt(0)))))

      when {
        broker.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some(amlsRegistrationNumber))

      whenReady(broker.get.value) { r => r mustBe Some(amlsRegistrationNumber) }

    }
  }

}
