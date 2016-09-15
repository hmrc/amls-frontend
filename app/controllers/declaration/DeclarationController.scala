package controllers.declaration

import config.AMLSAuthConnector
import connectors.{DESConnector, DataCacheConnector}
import controllers.BaseController
import models.SubscriptionResponse
import models.declaration.AddPerson
import models.status._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait DeclarationController extends BaseController {

  private[controllers] def desConnector: DESConnector
  def dataCacheConnector: DataCacheConnector

  def get() = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetchAll flatMap { cacheMapO =>
        (for {
          cache <- cacheMapO
          subscription <- cache.getEntry[SubscriptionResponse](SubscriptionResponse.key)
        } yield {
          cache.getEntry[AddPerson](AddPerson.key) match {
            case Some(addPerson) =>
              val name = s"${addPerson.firstName} ${addPerson.middleName mkString} ${addPerson.lastName}"
              etmpStatus(subscription.paymentReference)(hc, authcontext) flatMap {
                case SubmissionReadyForReview => Future.successful(Ok(views.html.declaration.declare(name)))
                case _ => Future.successful(Ok(views.html.declaration.declare(name)))
              }
            case _ =>
              Future.successful(Redirect(routes.AddPersonController.get()))
          }
        }) getOrElse Future.failed(new Exception("Failure to get subscription response from cache"))
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

}

object DeclarationController extends DeclarationController {
  // $COVERAGE-OFF$
  override private[controllers] val desConnector: DESConnector = DESConnector
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
