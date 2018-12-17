/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.BaseController
import forms.EmptyForm
import javax.inject.Inject
import models.tradingpremises.TradingPremises
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.tradingpremises.your_trading_premises

class YourTradingPremisesController @Inject()(val dataCacheConnector: DataCacheConnector,
                                              val statusService: StatusService,
                                              val authConnector: AuthConnector
                                             ) extends RepeatingSection with BaseController {

  def get() = Authorised.async {
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
        case (Some(data), status) => {
          val (completeTp, incompleteTp) = TradingPremises.filterWithIndex(data)
            .partition(_._1.isComplete)

          Ok(your_trading_premises(EmptyForm, false, status, completeTp, incompleteTp))
        }
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

}
