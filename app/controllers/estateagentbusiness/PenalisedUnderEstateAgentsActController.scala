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

package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.estateagentbusiness.{EstateAgentBusiness, PenalisedUnderEstateAgentsAct}
import views.html.estateagentbusiness._

import scala.concurrent.Future

trait PenalisedUnderEstateAgentsActController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            estateAgentsAct <- estateAgentBusiness.penalisedUnderEstateAgentsAct
          } yield Form2[PenalisedUnderEstateAgentsAct](estateAgentsAct)).getOrElse(EmptyForm)
          Ok(penalised_under_estate_agents_act(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[PenalisedUnderEstateAgentsAct](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(penalised_under_estate_agents_act(f, edit)))
        case ValidForm(_, data) =>
          for {estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key)
               _ <- dataCacheConnector.save[EstateAgentBusiness](
                 EstateAgentBusiness.key,
                 estateAgentBusiness.penalisedUnderEstateAgentsAct(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PenalisedByProfessionalController.get())
          }
      }
  }
}

object PenalisedUnderEstateAgentsActController extends PenalisedUnderEstateAgentsActController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
