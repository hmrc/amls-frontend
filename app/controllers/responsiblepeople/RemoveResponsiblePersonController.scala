package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.responsiblepeople.ResponsiblePeople
import models.status.SubmissionDecisionApproved
import services.StatusService
import utils.{StatusConstants, RepeatingSection}

import scala.concurrent.Future

trait RemoveResponsiblePersonController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  def get(index: Int, complete: Boolean = false) = Authorised.async {
      implicit authContext => implicit request =>
        for {
          rp <- getData[ResponsiblePeople](index)
          status <- statusService.getStatus
        } yield (rp, status) match {
          case (Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_)), SubmissionDecisionApproved) => {
            Ok(views.html.responsiblepeople.remove_responsible_person(
              EmptyForm, index, personName.fullName, complete, true))
          }
          case (Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_)),_) => {
            Ok(views.html.responsiblepeople.remove_responsible_person(
              EmptyForm, index, personName.fullName, complete, false))
          }
          case _ => NotFound(notFoundView)
        }
  }

  def remove(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      for {
        rs <- updateDataStrict[ResponsiblePeople](index) { rp =>
          rp.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
        }
      } yield Redirect(routes.CheckYourAnswersController.get())
    }
  }
}

object RemoveResponsiblePersonController extends RemoveResponsiblePersonController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService

}
