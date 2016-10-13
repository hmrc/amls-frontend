package services

import connectors.AmlsConnector
import models.ReadStatusResponse
import models.registrationprogress.{Completed, NotStarted, Section}
import models.status._
import org.joda.time.LocalDateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Call
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class StatusServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object TestStatusService extends StatusService {
    override private[services] val amlsConnector: AmlsConnector = mock[AmlsConnector]
    override private[services] val progressService: ProgressService = mock[ProgressService]
    override private[services] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
  }

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]
  implicit val ec = mock[ExecutionContext]

  val readStatusResponse:ReadStatusResponse = ReadStatusResponse(new LocalDateTime(),"Pending",None,None,None,false)

  "Status Service" must {
    "return NotCompleted" in {

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", NotStarted, false, Call("", "")))))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual NotCompleted
      }
    }

    "return SubmissionReady" in {

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual SubmissionReady
      }
    }

    "return SubmissionReadyForReview" in {

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(TestStatusService.amlsConnector.status(any())(any(),any(),any(),any())).thenReturn(Future.successful(readStatusResponse))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual SubmissionReadyForReview
      }
    }

    "return SubmissionDecisionApproved" in {

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(TestStatusService.amlsConnector.status(any())(any(),any(),any(),any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Approved")))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual SubmissionDecisionApproved
      }
    }

    "return SubmissionDecisionRejected" in {

      when(TestStatusService.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsref")))
      when(TestStatusService.progressService.sections(any(), any(), any())).thenReturn(Future.successful(Seq(Section("test", Completed, false, Call("", "")))))
      when(TestStatusService.amlsConnector.status(any())(any(),any(),any(),any())).thenReturn(Future.successful(readStatusResponse.copy(formBundleStatus = "Rejected")))
      whenReady(TestStatusService.getStatus) {
        _ mustEqual SubmissionDecisionRejected
      }
    }
  }

}
