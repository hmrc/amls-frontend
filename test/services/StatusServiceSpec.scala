/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Environment
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class StatusServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

   val service = new StatusService(
    amlsConnector = mock[AmlsConnector],
    enrolmentsService = mock[AuthEnrolmentsService],
    sectionsProvider = mock[SectionsProvider],
    environment = mock[Environment]
  )

  val amlsRegNo = Some("X0123456789")
  val accountTypeId = ("accountType", "accountId")
  val credId = "123412345"
  
  implicit val hc = mock[HeaderCarrier]
  implicit val ec = app.injector.instanceOf[ExecutionContext]

  val readStatusResponse: ReadStatusResponse = ReadStatusResponse(new LocalDateTime(), "Pending", None, None, None,
    None, false)

  "Status Service" must {
    "return NotCompleted" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(None))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", NotStarted, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse))
      whenReady(service.getStatus(None, accountTypeId, credId)) {
        _ mustEqual NotCompleted
      }
    }

    "return SubmissionReady" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(None))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse))
      whenReady(service.getStatus(None, accountTypeId, credId)) {
        _ mustEqual SubmissionReady
      }
    }

    "return SubmissionReadyForReview" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionReadyForReview
      }
    }

    "return SubmissionDecisionApproved" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved")))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionApproved
      }
    }

    "return SubmissionDecisionRejected" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Rejected")))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionRejected
      }
    }

    "return SubmissionDecisionRevoked" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Revoked")))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionRevoked
      }
    }

    "return SubmissionDecisionExpired" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Expired")))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionExpired
      }
    }

    "return ReadyForRenewal" in {
      val renewalDate = LocalDate.now().plusDays(15)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual ReadyForRenewal(Some(renewalDate))
      }

    }

    "return ReadyForRenewal when on first day of window" in {
      val renewalDate = new LocalDate(2017,3,31)
      DateTimeUtils.setCurrentMillisFixed((new LocalDate(2017,3,2)).toDateTimeAtStartOfDay.getMillis)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual ReadyForRenewal(Some(renewalDate))
      }

      DateTimeUtils.setCurrentMillisSystem()

    }

    "return Approved when one day before window" in {
      val renewalDate = new LocalDate(2017,3,31)
      DateTimeUtils.setCurrentMillisFixed((new LocalDate(2017,3,1)).toDateTimeAtStartOfDay.getMillis)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionApproved
      }

      DateTimeUtils.setCurrentMillisSystem()

    }

    "not return ReadyForRenewal" in {
      val renewalDate = LocalDate.now().plusDays(15)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "adasdasd", currentRegYearEndDate = Some(renewalDate))))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId).failed) {
        _.getMessage mustBe("ETMP returned status is inconsistent")
      }

    }

    "return RenewalSubmitted" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved",renewalConFlag = true)))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual RenewalSubmitted(None)
      }
    }

    "return SubmissionWithdrawn" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Withdrawal")))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionWithdrawn
      }
    }

    "return DeRegistered" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "De-Registered")))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual DeRegistered
      }
    }

    "return SafeId" in {
      val safeId = "J4JF8EJ3NDJWI32W"
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(service.sectionsProvider.sections(any[String]())(any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(service.amlsConnector.status(any(), any())(any(), any(), any())).thenReturn(Future.successful(readStatusResponse.copy(safeId = Some(safeId))))
      whenReady(service.getSafeIdFromReadStatus("amlsref", accountTypeId)) {
        _ mustEqual Some(safeId)
      }
    }
  }

  "getDetailedStatus" must {
    "return post-submission status if mlrRegNumber is defined" in {

      val result = service.getDetailedStatus(Some("regNo"), ("", ""), "credId")

      whenReady(result) {
        _._1 mustBe SubmissionReadyForReview
      }
    }

    "return pre-submission status if mlrRegNumber is not defined" in {

      val result = service.getDetailedStatus(None, ("", ""), "credId")

      whenReady(result) {
        _._1 mustBe SubmissionReady
      }
    }
  }

  "getReadStatus" must {
    "return etmpReadStatus if mlrRegNumber is defined" in {

      val result = service.getReadStatus(Some("regNo"), ("", ""))

      whenReady(result) {
        _.isInstanceOf[ReadStatusResponse]
      }
    }

    "throw an exception with message if mlrRegNumber is not defined" in {
      val ex = intercept[RuntimeException] {
        service.getReadStatus(None, ("", ""))
      }
      assert(ex.getMessage == "ETMP returned no read status")
    }
  }
}
