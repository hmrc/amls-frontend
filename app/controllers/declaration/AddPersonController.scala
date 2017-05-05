package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.status.SubmissionReadyForReview
import play.api.mvc.{AnyContent, Request, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.ControllerHelper

import scala.concurrent.Future

trait AddPersonController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get() = Authorised.async {
    implicit authContext => implicit request => {
          addPersonView(Ok,EmptyForm)
      }
  }

  def getWithAmendment() = get()

  def post() = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AddPerson](request.body) match {
        case f: InvalidForm =>
          addPersonView(BadRequest,f)
        case ValidForm(_, data) =>
          dataCacheConnector.save[AddPerson](AddPerson.key, data) flatMap { _ =>
            statusService.getStatus map {
              case SubmissionReadyForReview if AmendmentsToggle.feature => Redirect(routes.DeclarationController.getWithAmendment())
              case _ => Redirect(routes.DeclarationController.get())
            }
          }
      }
    }
  }

  private def addPersonView(status: Status, form: Form2[AddPerson])
                           (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] = {

    dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) flatMap { bm =>
      val businessType = ControllerHelper.getBusinessType(bm)
        statusService.getStatus map {
          case SubmissionReadyForReview if AmendmentsToggle.feature =>
            status(views.html.declaration.add_person("declaration.addperson.amendment.title", "submit.amendment.application", businessType, form))
          case _ => status(views.html.declaration.add_person("declaration.addperson.title", "submit.registration", businessType, form))
        }
      }
    }
}

object AddPersonController extends AddPersonController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
