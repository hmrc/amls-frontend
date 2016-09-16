package controllers.declaration

import config.AMLSAuthConnector
import connectors.{DESConnector, DataCacheConnector}
import controllers.BaseController
import models.SubscriptionResponse
import models.declaration.AddPerson
import models.status._
import play.api.i18n.Messages
import services.AuthEnrolmentsService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait DeclarationController extends BaseController {

  private[controllers] def desConnector: DESConnector
  def dataCacheConnector: DataCacheConnector
  def authEnrolmentsService: AuthEnrolmentsService

  def get() = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) flatMap {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName mkString} ${addPerson.lastName}"
          getAMLSRegNo flatMap {
            case Some(amlsRegNo) => etmpStatus(amlsRegNo)(hc, authcontext) flatMap {
              case SubmissionReadyForReview =>
                Future.successful(Ok(views.html.declaration.declare(Messages("submit.amendment.registration"), name)))
              case _ => Future.successful(Ok(views.html.declaration.declare(Messages("submit.registration"), name)))
            }
            case None => Future.successful(Ok(views.html.declaration.declare(Messages("submit.registration"), name)))
          }
        case _ => Future.successful(Redirect(routes.AddPersonController.get()))
      }
  }

  private def etmpStatus(amlsRefNumber: String)(implicit hc: HeaderCarrier, auth: AuthContext): Future[SubmissionStatus] = {
    {
      desConnector.status(amlsRefNumber) map {
        response => response.formBundleStatus match {
          case "Pending" => SubmissionReadyForReview
          case "Approved" => SubmissionDecisionApproved
          case "Rejected" => SubmissionDecisionRejected
          case _ => NotCompleted
        }
      }
    }
  }

  private def getAMLSRegNo(implicit hc: HeaderCarrier, auth: AuthContext): Future[Option[String]] =
    authEnrolmentsService.amlsRegistrationNumber

}

object DeclarationController extends DeclarationController {
  // $COVERAGE-OFF$
  override private[controllers] val desConnector: DESConnector = DESConnector
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService
}
