package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.TradingPremises
import utils.RepeatingSection
import views.html.tradingpremises._

trait SummaryController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Seq[TradingPremises]](TradingPremises.key) map {
        case Some(data) =>
          Ok(summary(data))
        case _ =>
          Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }

  def getIndividual(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(data) =>
          Ok(summary_2(data, index))
        case _ =>
          Redirect(routes.SummaryController.get)
      }
  }
}

object SummaryController extends SummaryController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}

