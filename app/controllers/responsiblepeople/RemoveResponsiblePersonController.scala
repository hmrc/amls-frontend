package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ResponsiblePeople, ResponsiblePersonEndDate}
import models.status.SubmissionDecisionApproved
import services.{AuthEnrolmentsService, StatusService}
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.Future

trait RemoveResponsiblePersonController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  private[controllers] def authEnrolmentsService: AuthEnrolmentsService

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        rp <- getData[ResponsiblePeople](index)
      } yield rp match {
        case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _)) => {
          Ok(views.html.responsiblepeople.remove_responsible_person(
            EmptyForm, index, personName.fullName, complete))
        }
        case _ => NotFound(notFoundView)
      }
  }


  def remove(index: Int,
             complete: Boolean = false,
             personName: String
            ) = Authorised.async {
    implicit authContext => implicit request =>

      authEnrolmentsService.amlsRegistrationNumber flatMap {
        case Some(_) => {
              for {
                result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  rp.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
                }
              } yield Redirect(routes.CheckYourAnswersController.get())
        }
        case _ => {
          removeDataStrict[ResponsiblePeople](index) map { _ =>
            Redirect(routes.CheckYourAnswersController.get())
          }
        }
      }
  }
}

object RemoveResponsiblePersonController extends RemoveResponsiblePersonController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService
  override private[controllers] val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService

}
