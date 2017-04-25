package services

import config.ApplicationConfig
import connectors.AmlsConnector
import models.ReadStatusResponse
import models.registrationprogress.{Completed, Section}
import models.status._
import org.joda.time.LocalDate
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait StatusService {

  private[services] def amlsConnector: AmlsConnector

  private[services] def progressService: ProgressService

  private[services] def enrolmentsService: AuthEnrolmentsService

  private val renewalPeriod = 30

  val Pending = "Pending"
  val Approved = "Approved"
  val Rejected = "Rejected"
  val Revoked = "Revoked"
  val Expired = "Expired"

  private def getApprovedStatus(response: ReadStatusResponse) = {
    (response.currentRegYearEndDate, response.renewalConFlag) match {
      case (Some(endDate), false) if ApplicationConfig.renewalsToggle && LocalDate.now().isAfter(endDate.minusDays(renewalPeriod)) =>
        ReadyForRenewal(response.currentRegYearEndDate)
      case (_, true) => RenewalSubmitted(response.currentRegYearEndDate)
      case _ => SubmissionDecisionApproved
    }
  }

  private def getETMPStatus(response: ReadStatusResponse) = {
    response.formBundleStatus match {
      case `Pending` => SubmissionReadyForReview
      case `Approved` => getApprovedStatus(response)
      case `Rejected` => SubmissionDecisionRejected
      case `Revoked` => SubmissionDecisionRevoked
      case `Expired` => SubmissionDecisionExpired
      case _ => throw new RuntimeException("ETMP returned status is inconsistent")
    }
  }

  private def etmpStatusInformation(mlrRegNumber: String)(implicit hc: HeaderCarrier,
                                                          auth: AuthContext, ec: ExecutionContext): Future[(SubmissionStatus, Option[ReadStatusResponse])] = {
    amlsConnector.status(mlrRegNumber) map {
      response =>
        val status = getETMPStatus(response)
        (status, Some(response))
    }
  }

  def getDetailedStatus(implicit hc: HeaderCarrier, authContext: AuthContext, ec: ExecutionContext): Future[(SubmissionStatus, Option[ReadStatusResponse])] = {
    enrolmentsService.amlsRegistrationNumber flatMap {
      case Some(mlrRegNumber) =>
        etmpStatusInformation(mlrRegNumber)(hc, authContext, ec)
      case None =>
        notYetSubmitted(hc, authContext, ec) map { status =>
          (status, None)
        }
    }
  }

  def getStatus(implicit hc: HeaderCarrier, authContext: AuthContext, ec: ExecutionContext): Future[SubmissionStatus] = {
    enrolmentsService.amlsRegistrationNumber flatMap {
      case Some(mlrRegNumber) =>
        etmpStatus(mlrRegNumber)(hc, authContext, ec)
      case None =>
        notYetSubmitted(hc, authContext, ec)
    }
  }

  private def notYetSubmitted(implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext) = {

    def isComplete(seq: Seq[Section]): Boolean =
      seq forall {
        _.status == Completed
      }

    progressService.sections map {
      sections =>
        if (isComplete(sections)) {
          SubmissionReady
        } else {
          NotCompleted
        }
    }
  }

  private def etmpStatus(amlsRefNumber: String)(implicit hc: HeaderCarrier, auth: AuthContext, ec: ExecutionContext): Future[SubmissionStatus] = {
    {
      amlsConnector.status(amlsRefNumber) map {
        response =>
          getETMPStatus(response)
      }
    }
  }
}

object StatusService extends StatusService {

  override private[services] val amlsConnector: AmlsConnector = AmlsConnector

  override private[services] val progressService: ProgressService = ProgressService

  override private[services] val enrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService
}
