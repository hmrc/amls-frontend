package controllers.tradingpremises

import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import play.api.mvc.{AnyContent, Request, Results}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{ControllerHelper, StatusConstants}

object TPControllerHelper {

  def redirectToNextPage(maybeCache: Option[CacheMap], index: Int, edit: Boolean)(implicit request: Request[AnyContent]) = {
    maybeCache map { cache =>

      val maybeTradingPremises = for {
        tp <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
      } yield tp collect {
        case t if !t.status.contains(StatusConstants.Deleted) => t
      }

      val isAgent = (for {
        tpList <- maybeTradingPremises
        tp <- tpList.headOption
      } yield tp.registeringAgentPremises.contains(RegisteringAgentPremises(true))) getOrElse false

      val isFirst = maybeTradingPremises.fold(0)(_.size) == 1

      isFirst match {
        case true if isAgent => Results.Redirect(routes.WhereAreTradingPremisesController.get(index, edit))
        case true if !edit => Results.Redirect(routes.ConfirmAddressController.get(index))
        case false => Results.Redirect(routes.WhereAreTradingPremisesController.get(index, edit))
      }

    } getOrElse Results.NotFound(ControllerHelper.notFoundView)
  }

}
