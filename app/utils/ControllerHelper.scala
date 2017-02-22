package utils

import models.businessmatching._
import models.responsiblepeople.{NominatedOfficer, ResponsiblePeople}
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ControllerHelper {

  def getBusinessType(matching: Option[BusinessMatching]): Option[BusinessType] = {
    matching flatMap { bm =>
      bm.reviewDetails match {
        case Some(review) => review.businessType
        case _ => None
      }
    }
  }

  def getMsbServices(matching: Option[BusinessMatching]): Option[Set[MsbService]] = {
    matching flatMap { bm =>
        bm.msbServices match {
          case Some(service) => Some(service.msbServices)
          case _ => None
        }
      }
  }

  def getBusinessActivity(matching: Option[BusinessMatching]): Option[BusinessActivities] = {
    matching match {
      case Some(data) => data.activities
      case None => None
    }
  }

  def isMSBSelected(bm: Option[BusinessMatching]): Boolean = {
    bm match {
      case Some(matching) => matching.activities.foldLeft(false) { (x, y) =>
        y.businessActivities.contains(MoneyServiceBusiness)
      }
      case None => false
    }
  }

  //For repeating section
  def allowedToEdit(edit: Boolean)(implicit statusService: StatusService, hc: HeaderCarrier, auth: AuthContext): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionReady | NotCompleted => true
      case _ => !edit
    }
  }

  def allowedToEdit(implicit statusService: StatusService, hc: HeaderCarrier, auth: AuthContext): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview  => true
      case _ => false
    }
  }

  def hasNominatedOfficer(eventualMaybePeoples: Future[Option[Seq[ResponsiblePeople]]]): Future[Boolean] = {
    eventualMaybePeoples map {
      case Some(rps) =>  rps.exists(_.positions.fold(false)(_.positions.contains(NominatedOfficer)))
      case _ =>  false
    }
  }
}
