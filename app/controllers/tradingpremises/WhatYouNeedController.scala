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
import models.businessmatching.BusinessActivity.MoneyServiceBusiness

import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import play.api.Logging
import views.html.tradingpremises.WhatYouNeedView

@Singleton
class WhatYouNeedController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  view: WhatYouNeedView
) extends AmlsBaseController(ds, cc)
    with Logging {

  def get(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
      (for {
        bm <- businessMatching
        ba <- bm.activities
      } yield {

        val call = if (ba.hasBusinessOrAdditionalActivity(MoneyServiceBusiness)) {
          controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(index)
        } else {
          if (index == 1) {
            controllers.tradingpremises.routes.ConfirmAddressController.get(index)
          } else {
            controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(index)
          }
        }

        Ok(view(call, index, Some(ba), bm.msbServices))
      }).getOrElse {
        logger.warn("Unable to retrieve business activities in [tradingpremises][WhatYouNeedController]")
        throw new Exception("Unable to retrieve business activities in [tradingpremises][WhatYouNeedController]")
      }
    }
  }
}
