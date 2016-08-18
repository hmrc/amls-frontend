package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.{BusinessMatching, MoneyServiceBusiness}
import models.tradingpremises.TradingPremises
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
            bm <- c.getEntry[BusinessMatching](BusinessMatching.key)
            tp <- c.getEntry[Seq[TradingPremises]](TradingPremises.key)
          } yield (bm, tp)
      } map {
        case Some(data) => Ok(summary(data._2,edit))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def getIndividual(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(data) =>
          Ok(summary_2(data, index))
        case _ =>
          NotFound(notFoundView)
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}

