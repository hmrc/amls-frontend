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

package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.estateagentbusiness.{ProfessionalBody, EstateAgentBusiness}
import views.html.estateagentbusiness._

import scala.concurrent.Future

trait PenalisedByProfessionalController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            professionalBody <- estateAgentBusiness.professionalBody
          } yield Form2[ProfessionalBody](professionalBody)).getOrElse(EmptyForm)
          Ok(penalised_by_professional(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ProfessionalBody](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(penalised_by_professional(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key)
            _ <- dataCacheConnector.save[EstateAgentBusiness](EstateAgentBusiness.key,
              estateAgentBusiness.professionalBody(data))
          } yield Redirect(routes.SummaryController.get())
      }
  }
}

object PenalisedByProfessionalController extends PenalisedByProfessionalController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
