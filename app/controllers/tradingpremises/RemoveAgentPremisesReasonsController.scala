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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.tradingpremises.RemoveAgentPremisesReasonsFormProvider
import models.tradingpremises.{AgentRemovalReason, TradingPremises}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.RemoveAgentPremisesReasonsView

import scala.concurrent.Future

class RemoveAgentPremisesReasonsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: RemoveAgentPremisesReasonsFormProvider,
  view: RemoveAgentPremisesReasonsView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, complete: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    for {
      tp <- getData[TradingPremises](request.credId, index)
    } yield tp match {
      case (Some(tradingPremises)) =>
        val form = tradingPremises.removalReason.fold(formProvider())(reason =>
          formProvider().fill(AgentRemovalReason(reason, tradingPremises.removalReasonOther))
        )
        Ok(view(form, index, complete))

      case _ => NotFound(notFoundView)
    }
  }

  def post(index: Int, complete: Boolean = false): Action[AnyContent] =
    authAction.async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, index, complete))),
          data =>
            updateDataStrict[TradingPremises](request.credId, index) {
              _.copy(
                removalReason = Some(data.removalReason),
                removalReasonOther = data.removalReasonOther
              )
            } map { _ =>
              Redirect(controllers.tradingpremises.routes.RemoveTradingPremisesController.get(index, complete))
            }
        )
    }

}
