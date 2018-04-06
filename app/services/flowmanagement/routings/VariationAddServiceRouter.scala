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

package services.flowmanagement.routings

import connectors.DataCacheConnector
import models.businessmatching._
import models.businessmatching.updateservice._
import models.flowmanagement._
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import controllers.businessmatching.updateservice.routes
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import services.flowmanagement.Router

import scala.concurrent.Future

object VariationAddServiceRouter {

  // scalastyle:off cyclomatic.complexity
  implicit val router = new Router[AddServiceFlowModel] {

    override def getRoute(pageId: PageId, model: AddServiceFlowModel, edit: Boolean = false)(implicit dataCacheConnector: DataCacheConnector): Future[Result] = pageId match {

      case SelectActivitiesPageId if edit && model.areNewActivitiesAtTradingPremises.isDefined =>
        Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))

      case SelectActivitiesPageId =>
        Future.successful(Redirect(addRoutes.TradingPremisesController.get()))

      case TradingPremisesPageId if edit && model.tradingPremisesActivities.isDefined =>
        Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))

      case TradingPremisesPageId =>
        model.areNewActivitiesAtTradingPremises match {
          case Some(true) =>
            Future.successful(Redirect(addRoutes.WhichTradingPremisesController.get()))
          case _ =>
            Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))
        }

      case WhichTradingPremisesPageId => Future.successful(Redirect(addRoutes.UpdateServicesSummaryController.get()))

      case UpdateServiceSummaryPageId => Future.successful(Redirect(addRoutes.AddMoreActivitiesController.get()))

      case AddMoreAcivitiesPageId => model.addMoreActivities match {
        case Some(true) =>
          Future.successful(Redirect(addRoutes.SelectActivitiesController.get()))

        case _ =>
          if (model.informationRequired) {
            Future.successful(Redirect(addRoutes.NewServiceInformationController.get()))
          } else {
            Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
          }
      }

      case NewServiceInformationPageId => Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
    }
  }
}
