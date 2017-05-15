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

package services

import connectors.AmlsConnector
import models.ReadStatusResponse
import models.registrationprogress.{Completed, NotStarted, Section}
import models.status._
import org.joda.time.{DateTimeUtils, LocalDate, LocalDateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.Call
import play.api.test.FakeApplication
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class StatusServicePreRenewalsSpec extends PlaySpec with MockitoSugar with ScalaFutures with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.renewals" -> false) )

  object TestStatusService extends StatusService {
    override private[services] val amlsConnector: AmlsConnector = mock[AmlsConnector]
    override private[services] val progressService: ProgressService = mock[ProgressService]
    override private[services] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
  }

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]
  implicit val ec = mock[ExecutionContext]

  val readStatusResponse: ReadStatusResponse = ReadStatusResponse(new LocalDateTime(), "Pending", None, None, None, None, false)

  "Status Service Pre Renewals" must {


    "return SubmissionDecisionApproved" in {
      val renewalDate = LocalDate.now().plusDays(15)

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(TestStatusService.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual SubmissionDecisionApproved
      }

    }

    "return SubmissionDecisionApproved when on first day of window" in {
      val renewalDate = new LocalDate(2017,3,31)
      DateTimeUtils.setCurrentMillisFixed((new LocalDate(2017,3,2)).toDateTimeAtStartOfDay.getMillis)

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(TestStatusService.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual SubmissionDecisionApproved
      }

      DateTimeUtils.setCurrentMillisSystem()

    }

    "return SubmissionDecisionApproved when one day before window" in {
      val renewalDate = new LocalDate(2017,3,31)
      DateTimeUtils.setCurrentMillisFixed((new LocalDate(2017,3,1)).toDateTimeAtStartOfDay.getMillis)

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(TestStatusService.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual SubmissionDecisionApproved
      }

      DateTimeUtils.setCurrentMillisSystem()

    }

    
  }

}
