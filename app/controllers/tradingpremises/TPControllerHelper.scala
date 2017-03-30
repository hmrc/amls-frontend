package controllers.tradingpremises

import models.tradingpremises.TradingPremises
import play.api.mvc.{AnyContent, Request, Results}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{ControllerHelper, StatusConstants}

object TPControllerHelper {

  def isFirstTradingPremises(cache: CacheMap): Option[Boolean] = {
    cache.getEntry[Seq[TradingPremises]](TradingPremises.key) map {tps =>
      tps.filterNot(_.status.contains(StatusConstants.Deleted)).size == 1
    }
  }

  def redirectToNextPage(result: Option[CacheMap], index: Int, edit: Boolean)(implicit request: Request[AnyContent] )= {
    result match {
      case Some(cache) => isFirstTradingPremises(cache).getOrElse(false) match {
        case true if !edit => Results.Redirect(routes.ConfirmAddressController.get(index))
        case false => Results.Redirect(routes.WhereAreTradingPremisesController.get(index, edit))
      }
      case _ => Results.NotFound(ControllerHelper.notFoundView)
    }
  }

}
