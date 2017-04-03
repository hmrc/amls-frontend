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


  def etmpStatusInformation(mlrRegNumber: String) (implicit hc: HeaderCarrier,
                                                   auth: AuthContext, ec: ExecutionContext) : Future[(SubmissionStatus, Option[ReadStatusResponse])] = {
    amlsConnector.status(mlrRegNumber) map {
      response =>
        (response.formBundleStatus, response.currentRegYearEndDate, response.renewalConFlag) match {
          case ("Pending", None, false) => (SubmissionReadyForReview, Some(response))
          case ("Rejected", None, false) => (SubmissionDecisionRejected, Some(response))
          case ("Approved", Some(endDate), false) if ApplicationConfig.renewalsToggle && LocalDate.now().isAfter(endDate.minusDays(renewalPeriod))
          => (ReadyForRenewal(response.currentRegYearEndDate), Some(response))
          case ("Approved", _, true) => (RenewalSubmitted(response.currentRegYearEndDate), Some(response))
          case ("Approved", _, false) => (SubmissionDecisionApproved, Some(response))
          case _ => throw new RuntimeException("ETMP returned status is inconsistent")
        }
    }
  }

  def getDetailedStatus(implicit hc: HeaderCarrier, authContext: AuthContext, ec: ExecutionContext): Future[(SubmissionStatus, Option[ReadStatusResponse])] = {
    enrolmentsService.amlsRegistrationNumber flatMap {
      case Some(mlrRegNumber) =>
        etmpStatusInformation(mlrRegNumber)(hc, authContext, ec)
      case None =>
        notYetSubmitted(hc, authContext, ec) map {status =>
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
          (response.formBundleStatus, response.currentRegYearEndDate, response.renewalConFlag) match {
            case ("Pending", None, false) => SubmissionReadyForReview
            case ("Rejected", None, false) => SubmissionDecisionRejected
            case ("Approved", Some(endDate), false) if ApplicationConfig.renewalsToggle && LocalDate.now().isAfter(endDate.minusDays(renewalPeriod))
            => ReadyForRenewal(response.currentRegYearEndDate)
            case ("Approved", _, true) => RenewalSubmitted(response.currentRegYearEndDate)
            case ("Approved", _, false) => SubmissionDecisionApproved
            case _ => throw new RuntimeException("ETMP returned status is inconsistent")
          }
      }
    }
  }
}

object StatusService extends StatusService {

  override private[services] val amlsConnector: AmlsConnector = AmlsConnector

  override private[services] val progressService: ProgressService = ProgressService

  override private[services] val enrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService
}
