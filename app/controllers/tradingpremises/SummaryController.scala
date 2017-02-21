package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import models.status._
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.RepeatingSection
import views.html.tradingpremises._

trait SummaryController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector
  def statusService: StatusService

  def get(edit:Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        status <- statusService.getStatus
        tp <- dataCacheConnector.fetchAll map {
          cache =>
            for {
              c: CacheMap <- cache
              tp <- c.getEntry[Seq[TradingPremises]](TradingPremises.key)
            } yield tp
        }
      } yield (tp, status)) map {
        case (Some(data), status) => Ok(summary(data, edit, status))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def answers = get(true)

  def getIndividual(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(data) =>
          Ok(summary_details(data, index))
        case _ =>
          NotFound(notFoundView)
      }
  }
}

object ModelHelpers {
  implicit class removeUrl(model: TradingPremises) {

    private def isSubmission(status: SubmissionStatus) = Set(NotCompleted, SubmissionReady, SubmissionReadyForReview).contains(status)

    def removeUrl(index: Int, complete: Boolean = false, status: SubmissionStatus): String = model.registeringAgentPremises match {
      case Some(RegisteringAgentPremises(true)) if ApplicationConfig.release7 && !isSubmission(status) && model.lineId.isDefined =>
        controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(index, complete).url
      case _ =>
        controllers.tradingpremises.routes.RemoveTradingPremisesController.get(index, complete).url
    }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
  // $COVERAGE-ON$
}

