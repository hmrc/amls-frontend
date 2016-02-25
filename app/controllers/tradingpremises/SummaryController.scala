package controllers.tradingpremises
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.TradingPremises


trait SummaryController extends BaseController {

  protected def dataCacheConnector: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](TradingPremises.key) map {
        case Some(data) => Ok(views.html.trading_premises_summary(data))
        case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object SummaryController extends SummaryController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}

