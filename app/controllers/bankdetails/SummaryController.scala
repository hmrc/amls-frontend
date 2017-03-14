package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.StatusConstants

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector
  val statusService: StatusService

  def get(complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        bankDetails <- dataCache.fetch[Seq[BankDetails]](BankDetails.key)
        status <- statusService.getStatus
      } yield bankDetails match {
        case Some(data) => {
          val canEdit = status match {
            case NotCompleted | SubmissionReady => true
            case _ => false
          }
          val bankDetails = data.filterNot(_.status.contains(StatusConstants.Deleted))
          Ok(views.html.bankdetails.summary(data, complete, hasBankAccount(bankDetails), canEdit, status))
        }
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  private def hasBankAccount(bankDetails: Seq[BankDetails]): Boolean = {
    bankDetails.exists(_.bankAccount.isDefined)
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
