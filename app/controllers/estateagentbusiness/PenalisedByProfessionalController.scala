/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.estateagentbusiness.{EstateAgentBusiness, ProfessionalBody}
import utils.AuthAction
import views.html.estateagentbusiness._

import scala.concurrent.Future

class PenalisedByProfessionalController @Inject()(
                                                   val authAction: AuthAction,
                                                   val dataCacheConnector: DataCacheConnector) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](request.credId, EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            professionalBody <- estateAgentBusiness.professionalBody
          } yield Form2[ProfessionalBody](professionalBody)).getOrElse(EmptyForm)
          Ok(penalised_by_professional(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[ProfessionalBody](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(penalised_by_professional(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](request.credId,EstateAgentBusiness.key)
            _ <- dataCacheConnector.save[EstateAgentBusiness](request.credId, EstateAgentBusiness.key,
              estateAgentBusiness.professionalBody(data))
          } yield Redirect(routes.SummaryController.get())
      }
  }
}