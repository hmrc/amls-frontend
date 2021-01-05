/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.{AgentRemovalReason, TradingPremises}
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.remove_agent_premises_reasons

import scala.concurrent.Future


class RemoveAgentPremisesReasonsController @Inject () (
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val authAction: AuthAction,
                                                      val ds: CommonPlayDependencies,
                                                      val cc: MessagesControllerComponents,
                                                      remove_agent_premises_reasons: remove_agent_premises_reasons,
                                                      implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, complete: Boolean = false) = authAction.async {
      implicit request =>
        for {
          tp <- getData[TradingPremises](request.credId, index)
        } yield tp match {
          case (Some(tradingPremises)) => {
            Ok(remove_agent_premises_reasons(EmptyForm, index, complete))
          }
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, complete: Boolean = false) =
    authAction.async {
        implicit request =>
          Form2[AgentRemovalReason](request.body) match {
            case form: InvalidForm => Future.successful(
              BadRequest(remove_agent_premises_reasons(form, index, complete)))

            case ValidForm(_, data) => updateDataStrict[TradingPremises](request.credId, index) {
              _.copy(
                removalReason = Some(data.removalReason),
                removalReasonOther = data.removalReasonOther
              )
            } map { _ =>
              Redirect(controllers.tradingpremises.routes.RemoveTradingPremisesController.get(index, complete))
            }
          }
    }

}