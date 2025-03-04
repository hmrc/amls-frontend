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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.flowmanagement.{RemoveBusinessTypeFlowModel, RemoveBusinessTypesSummaryPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.flowmanagement.Router
import utils.AuthAction
import views.html.businessmatching.updateservice.remove.RemoveActivitiesSummaryView

import javax.inject.Inject
import scala.concurrent.Future

class RemoveBusinessTypesSummaryController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val helper: RemoveBusinessTypeHelper,
  val router: Router[RemoveBusinessTypeFlowModel],
  val cc: MessagesControllerComponents,
  view: RemoveActivitiesSummaryView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    {
      for {
        flow <- OptionT(
                  dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key)
                )
      } yield Ok(view(flow))
    } getOrElse InternalServerError("Unable to get the flow model")
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    {
      for {
        model <- updateSubscription(request.credId)
        route <- OptionT.liftF(router.getRoute(request.credId, RemoveBusinessTypesSummaryPageId, model))
      } yield route
    } getOrElse InternalServerError("Unable to remove the business type")
  }

  private def updateSubscription(credId: String): OptionT[Future, RemoveBusinessTypeFlowModel] = for {
    model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](credId, RemoveBusinessTypeFlowModel.key))
    _     <- helper.removeFitAndProper(credId, model)
    _     <- helper.removeBusinessMatchingBusinessTypes(credId, model)
    _     <- helper.removeTradingPremisesBusinessTypes(credId, model)
    _     <- helper.removeSectionData(credId, model)
  } yield model
}
