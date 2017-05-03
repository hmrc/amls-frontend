package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import models.status.{ReadyForRenewal, SubmissionDecisionApproved}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait DetailedAnswersController extends BaseController {

  protected def dataCache: DataCacheConnector
  protected def statusService: StatusService

  private def showHideAddressMove()(implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionDecisionApproved | ReadyForRenewal(_) => true
      case _ => false
    }
  }

  def get(index: Int, fromYourAnswers: Boolean) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) flatMap {
          case Some(data) => {
            data.lift(index - 1) match {
              case Some(x) => showHideAddressMove map {showHide =>
                Ok(views.html.responsiblepeople.detailed_answers(Some(x), index, fromYourAnswers, showHide))
              }
              case _ => Future.successful(NotFound(notFoundView))
            }
          }
          case _ => Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
        }
    }
}

object DetailedAnswersController extends DetailedAnswersController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override protected def statusService: StatusService = StatusService
}
