package controllers.declaration

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import models.declaration.AddPerson
import models.status.SubmissionReadyForReview
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait DeclarationController extends BaseController {

  def dataCacheConnector: DataCacheConnector
  def statusService: StatusService

  def get(): Action[AnyContent] = declarationView(("declaration.declaration.title","submit.registration"))
  def getWithAmendment() = AmendmentsToggle.feature match {
    case true => declarationView(("declaration.declaration.amendment.title","submit.amendment.application"))
    case false => Redirect(routes.DeclarationController.get())
  }

  private def declarationView(headings: (String,String)) = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) flatMap {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName mkString} ${addPerson.lastName}"
          Future.successful(Ok(views.html.declaration.declare(headings, name)))
        case _ =>
          redirectToAddPersonPage
      }
  }

  private def redirectToAddPersonPage(implicit hc: HeaderCarrier, auth: AuthContext): Future[Result] =
    statusService.getStatus map {
      case SubmissionReadyForReview if AmendmentsToggle.feature => Redirect(routes.AddPersonController.getWithAmendment())
      case _ => Redirect(routes.AddPersonController.get())
    }

}

object DeclarationController extends DeclarationController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
