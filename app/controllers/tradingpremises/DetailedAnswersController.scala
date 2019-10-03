/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Singleton
import models.businessmatching.BusinessMatching
import models.tradingpremises.TradingPremises
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.tradingpremises.summary_details

import scala.concurrent.Future

@Singleton
class DetailedAnswersController @Inject()(val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val dataCacheConnector: DataCacheConnector,
                                          val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int) = authAction.async {
    implicit request =>
      (for {
        cache <- OptionT(dataCacheConnector.fetchAll(request.credId))
        tp <- OptionT.fromOption[Future](getData[TradingPremises](cache, index))
        bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
      } yield {
        val hasOneService = bm.activities.fold(false)(_.businessActivities.size == 1)
        val hasOneMsbService = bm.msbServices.fold(false)(_.msbServices.size == 1)

        Ok(summary_details(tp, ControllerHelper.isMSBSelected(Some(bm)), index, hasOneService, hasOneMsbService))

      }).getOrElse(NotFound(notFoundView))
  }

  def post(index: Int) = authAction.async{
    implicit request =>
      updateDataStrict[TradingPremises](request.credId, index){ tp =>
        tp.copy(hasAccepted = true)
      } flatMap { _ =>
         Future.successful(Redirect(controllers.tradingpremises.routes.YourTradingPremisesController.get()))
        }
      }

}
