package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.{BaseController, declaration}
import models.responsiblepeople.ResponsiblePeople
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import utils.ControllerHelper
import views.html.responsiblepeople._

import scala.concurrent.Future

trait CheckYourAnswersController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService : StatusService

  def get(fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
          case Some(data) => Ok(check_your_answers(data, fromDeclaration))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
    }

  def post(fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        fromDeclaration match {
          case false => Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
          case true => {
            for {
              status <- statusService.getStatus
              hasNominatedOfficer <- ControllerHelper.hasNominatedOfficer(dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
            } yield status match {
              case SubmissionReady | NotCompleted | SubmissionReadyForReview => {
                hasNominatedOfficer match {
                  case true => Redirect(controllers.declaration.routes.WhoIsRegisteringController.get())
                  case false => Redirect(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get())
                }
              }
              case _ => {
                hasNominatedOfficer match {
                  case true => Redirect(controllers.declaration.routes.WhoIsRegisteringController.getWithAmendment())
                  case false => Redirect(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment())
                }
              }
            }
          }
        }

    }
}

object CheckYourAnswersController extends CheckYourAnswersController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
