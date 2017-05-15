/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
