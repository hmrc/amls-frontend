package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.RepeatingSection
import views.html.tradingpremises._

trait SummaryController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit:Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
        cache =>
          for {
            c: CacheMap <- cache
            tp <- c.getEntry[Seq[TradingPremises]](TradingPremises.key)
          } yield tp
      } map {
        case Some(data) => Ok(summary(data,edit))
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
    def removeUrl(index: Int, complete: Boolean = false): String = model.registeringAgentPremises match {
      case Some(RegisteringAgentPremises(true)) =>
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
}

