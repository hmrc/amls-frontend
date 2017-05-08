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

  lazy val defaultView = declarationView("declaration.declaration.title", "submit.registration", isAmendment = false)

  def get(): Action[AnyContent] = defaultView

  def getWithAmendment = AmendmentsToggle.feature match {
    case b@true => declarationView("declaration.declaration.amendment.title", "submit.amendment.application", b)
    case _ => defaultView
  }

  private def declarationView(title: String, subtitle: String, isAmendment: Boolean) = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) flatMap {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName getOrElse ""} ${addPerson.lastName}"
          Future.successful(Ok(views.html.declaration.declare(title, subtitle, name, isAmendment)))
        case _ => redirectToAddPersonPage
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
