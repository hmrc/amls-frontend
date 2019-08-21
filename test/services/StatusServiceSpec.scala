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

import connectors.AmlsConnector
import controllers.actions.SuccessfulAuthAction
import models.ReadStatusResponse
import models.registrationprogress.{Completed, NotStarted, Section}
import models.status._
import org.joda.time.{DateTimeUtils, LocalDate, LocalDateTime}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class StatusServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with OneAppPerSuite {

   val service = new StatusService(
    amlsConnector = mock[AmlsConnector],
    enrolmentsService = mock[AuthEnrolmentsService],
    sectionsProvider = mock[SectionsProvider]
  )

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]
  implicit val ec = mock[ExecutionContext]

  val readStatusResponse: ReadStatusResponse = ReadStatusResponse(new LocalDateTime(), "Pending", None, None, None,
    None, false)

  "Status Service" must {
    "return NotCompleted" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", NotStarted, false, Call("", "")))))
      whenReady(service.getStatus) {
        _ mustEqual NotCompleted
      }
    }

    "return SubmissionReady" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionReady
      }
    }

    "return SubmissionReadyForReview" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionReadyForReview
      }
    }

    "return SubmissionDecisionApproved" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved")))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionDecisionApproved
      }
    }

    "return SubmissionDecisionRejected" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Rejected")))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionDecisionRejected
      }
    }

    "return SubmissionDecisionRevoked" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Revoked")))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionDecisionRevoked
      }
    }

    "return SubmissionDecisionExpired" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Expired")))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionDecisionExpired
      }
    }

    "return ReadyForRenewal" in {
      val renewalDate = LocalDate.now().plusDays(15)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus) {
        _ mustEqual ReadyForRenewal(Some(renewalDate))
      }

    }

    "return ReadyForRenewal when on first day of window" in {
      val renewalDate = new LocalDate(2017,3,31)
      DateTimeUtils.setCurrentMillisFixed((new LocalDate(2017,3,2)).toDateTimeAtStartOfDay.getMillis)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus) {
        _ mustEqual ReadyForRenewal(Some(renewalDate))
      }

      DateTimeUtils.setCurrentMillisSystem()

    }

    "return Approved when one day before window" in {
      val renewalDate = new LocalDate(2017,3,31)
      DateTimeUtils.setCurrentMillisFixed((new LocalDate(2017,3,1)).toDateTimeAtStartOfDay.getMillis)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionDecisionApproved
      }

      DateTimeUtils.setCurrentMillisSystem()

    }

    "not return ReadyForRenewal" in {
      val renewalDate = LocalDate.now().plusDays(15)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "adasdasd", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus.failed) {
        _.getMessage mustBe("ETMP returned status is inconsistent")
      }

    }

    "return RenewalSubmitted" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved",renewalConFlag = true)))
      whenReady(service.getStatus) {
        _ mustEqual RenewalSubmitted(None)
      }
    }

    "return SubmissionWithdrawn" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Withdrawal")))
      whenReady(service.getStatus) {
        _ mustEqual SubmissionWithdrawn
      }
    }

    "return DeRegistered" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "De-Registered")))
      whenReady(service.getStatus) {
        _ mustEqual DeRegistered
      }
    }

    "return SafeId" in {
      val safeId = "J4JF8EJ3NDJWI32W"
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any())(any(), any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(safeId = Some(safeId))))
      whenReady(service.getSafeIdFromReadStatus("amlsref")) {
        _ mustEqual Some(safeId)
      }
    }
  }
}
