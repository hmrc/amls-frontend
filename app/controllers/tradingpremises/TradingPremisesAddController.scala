/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import models.tradingpremises.TradingPremises
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.cache.Cache
import utils.{AuthAction, ControllerHelper, RepeatingSection}

import scala.concurrent.Future

@Singleton
class TradingPremisesAddController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  private def isMSBSelected(cacheMap: Option[Cache]): Boolean = {
    val test = for {
      c                <- cacheMap
      businessMatching <- c.getEntry[BusinessMatching](BusinessMatching.key)
    } yield businessMatching
    ControllerHelper.isMSBSelected(test)
  }

  def redirectToNextPage(credId: String, idx: Int)(implicit request: Request[AnyContent]) =
    dataCacheConnector.fetchAll(credId).map { cache =>
      isMSBSelected(cache) match {
        case true  => Redirect(controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(idx))
        case false => TPControllerHelper.redirectToNextPage(cache, idx, false)
      }
    }

  def get(displayGuidance: Boolean = true) = authAction.async { implicit request =>
    addData[TradingPremises](request.credId, TradingPremises.default(None)) flatMap { idx =>
      displayGuidance match {
        case true  => Future.successful(Redirect(controllers.tradingpremises.routes.WhatYouNeedController.get(idx)))
        case false => redirectToNextPage(request.credId, idx)
      }
    }
  }
}
