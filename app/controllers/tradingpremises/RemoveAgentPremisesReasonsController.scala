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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.{AgentRemovalReason, TradingPremises}
import services.StatusService
import utils.RepeatingSection
import views.html.tradingpremises.remove_agent_premises_reasons

import scala.concurrent.Future

trait RemoveAgentPremisesReasonsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        tp <- getData[TradingPremises](index)
      } yield tp match {
        case (Some(tradingPremises)) => {
          Ok(views.html.tradingpremises.remove_agent_premises_reasons(EmptyForm, index, complete))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, complete: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[AgentRemovalReason](request.body) match {
          case form: InvalidForm => Future.successful(
            BadRequest(remove_agent_premises_reasons(form, index, complete)))

          case ValidForm(_, data) => updateDataStrict[TradingPremises](index) { _.copy(
              removalReason = Some(data.removalReason),
              removalReasonOther = data.removalReasonOther
            )
          } map { _ =>
            Redirect(controllers.tradingpremises.routes.RemoveTradingPremisesController.get(index, complete))
          }
        }
    }

}


object RemoveAgentPremisesReasonsController extends RemoveAgentPremisesReasonsController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService
  //override private[controllers] val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService

}

