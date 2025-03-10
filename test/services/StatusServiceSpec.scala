/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.{AmlsConnector, DataCacheConnector}
import models.ReadStatusResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, NotStarted, TaskRow, Updated}
import models.status._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import play.api.{Application, Environment}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.ZoneOffset.UTC
import java.time._
import scala.concurrent.{ExecutionContext, Future}

class StatusServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .build()

  val mockClock: Clock = mock[Clock]

  val service = new StatusService(
    amlsConnector = mock[AmlsConnector],
    dataCacheConnector = mock[DataCacheConnector],
    enrolmentsService = mock[AuthEnrolmentsService],
    sectionsProvider = mock[SectionsProvider],
    environment = mock[Environment],
    clock = mockClock
  )

  val amlsRegNo     = Some("X0123456789")
  val accountTypeId = ("accountType", "accountId")
  val credId        = "123412345"

  implicit val hc: HeaderCarrier    = mock[HeaderCarrier]
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val messages: Messages   = Helpers.stubMessages()

  val readStatusResponse: ReadStatusResponse =
    ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, None, false)

  "Status Service" must {
    "return NotCompleted" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, NotStarted, TaskRow.notStartedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse))

      whenReady(service.getStatus(None, accountTypeId, credId)) {
        _ mustEqual NotCompleted
      }
    }

    "return SubmissionReady" when {

      "task rows have Completed status" in {

        when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(service.sectionsProvider.taskRows(any[String])(any(), any()))
          .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

        when(service.amlsConnector.status(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(readStatusResponse))
        whenReady(service.getStatus(None, accountTypeId, credId)) {
          _ mustEqual SubmissionReady
        }
      }

      "task rows have Updated status" in {

        when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(service.sectionsProvider.taskRows(any[String])(any(), any()))
          .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", true, Updated, TaskRow.updatedTag))))

        when(service.amlsConnector.status(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(readStatusResponse))
        whenReady(service.getStatus(None, accountTypeId, credId)) {
          _ mustEqual SubmissionReady
        }
      }

      "task rows have a combination of Completed and Updated status" in {

        when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(service.sectionsProvider.taskRows(any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              Seq(
                TaskRow("test", "/foo", false, Completed, TaskRow.completedTag),
                TaskRow("test2", "/bar", true, Updated, TaskRow.updatedTag)
              )
            )
          )

        when(service.amlsConnector.status(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(readStatusResponse))
        whenReady(service.getStatus(None, accountTypeId, credId)) {
          _ mustEqual SubmissionReady
        }
      }
    }

    "return SubmissionReadyForReview" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse))

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionReadyForReview
      }
    }

    "return SubmissionDecisionApproved" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved")))

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionApproved
      }
    }

    "return SubmissionDecisionRejected" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Rejected")))

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionRejected
      }
    }

    "return SubmissionDecisionRevoked" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Revoked")))

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionRevoked
      }
    }

    "return SubmissionDecisionExpired" in {

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Expired")))
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionExpired
      }
    }

    "return ReadyForRenewal" in {
      val renewalDate = LocalDate.now().plusDays(15)
      when(mockClock.withZone(UTC)).thenReturn(Clock.fixed(Instant.from(ZonedDateTime.now(UTC)), UTC))

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))
          )
        )
      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual ReadyForRenewal(Some(renewalDate))
      }
    }

    "return ReadyForRenewal when on first day of window" in {
      val renewalDate = LocalDate.of(2017, 3, 31)
      when(mockClock.withZone(UTC))
        .thenReturn(Clock.fixed(Instant.from(ZonedDateTime.of(2017, 3, 2, 0, 0, 0, 0, UTC)), UTC))

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))
          )
        )

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual ReadyForRenewal(Some(renewalDate))
      }
    }

    "return Approved when one day before window" in {
      val renewalDate = LocalDate.of(2017, 3, 31)
      when(mockClock.withZone(UTC))
        .thenReturn(Clock.fixed(Instant.from(ZonedDateTime.of(2017, 3, 1, 0, 0, 0, 0, UTC)), UTC))

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            readStatusResponse.copy(formBundleStatus = "Approved", currentRegYearEndDate = Some(renewalDate))
          )
        )

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionDecisionApproved
      }
    }

    "not return ReadyForRenewal" in {
      val renewalDate = LocalDate.now().plusDays(15)

      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            readStatusResponse.copy(formBundleStatus = "adasdasd", currentRegYearEndDate = Some(renewalDate))
          )
        )

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId).failed) {
        _.getMessage mustBe "ETMP returned status is inconsistent [status:adasdasd]"
      }

    }

    "return RenewalSubmitted" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved", renewalConFlag = true)))

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual RenewalSubmitted(None)
      }
    }

    "return SubmissionWithdrawn" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Withdrawal")))

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual SubmissionWithdrawn
      }
    }

    "return DeRegistered" in {
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "De-Registered")))

      whenReady(service.getStatus(amlsRegNo, accountTypeId, credId)) {
        _ mustEqual DeRegistered
      }
    }

    "return SafeId" in {
      val safeId = "J4JF8EJ3NDJWI32W"
      when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some("amlsref")))

      when(service.sectionsProvider.taskRows(any[String])(any(), any()))
        .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

      when(service.amlsConnector.status(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(readStatusResponse.copy(safeId = Some(safeId))))

      when(service.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Option.empty[BusinessMatching]))

      whenReady(service.getSafeIdFromReadStatus("amlsref", accountTypeId, credId)) {
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

    "return pre-submission status" when {

      "mlrRegNumber is not defined and row status is Completed" in {

        when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(service.sectionsProvider.taskRows(any[String])(any(), any()))
          .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", false, Completed, TaskRow.completedTag))))

        val result = service.getDetailedStatus(None, ("", ""), "credId")

        whenReady(result) {
          _._1 mustBe SubmissionReady
        }
      }

      "mlrRegNumber is not defined and row status is Updated" in {

        when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(service.sectionsProvider.taskRows(any[String])(any(), any()))
          .thenReturn(Future.successful(Seq(TaskRow("test", "/foo", true, Updated, TaskRow.updatedTag))))

        val result = service.getDetailedStatus(None, ("", ""), "credId")

        whenReady(result) {
          _._1 mustBe SubmissionReady
        }
      }

      "mlrRegNumber is not defined and row status is a combination of Completed and Updated" in {

        when(service.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(service.sectionsProvider.taskRows(any[String])(any(), any()))
          .thenReturn(
            Future.successful(
              Seq(
                TaskRow("test", "/foo", false, Completed, TaskRow.completedTag),
                TaskRow("test2", "/bar", true, Updated, TaskRow.updatedTag)
              )
            )
          )

        val result = service.getDetailedStatus(None, ("", ""), "credId")

        whenReady(result) {
          _._1 mustBe SubmissionReady
        }
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
