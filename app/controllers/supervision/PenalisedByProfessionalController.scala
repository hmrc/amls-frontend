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

package controllers.supervision

import javax.inject.Inject

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.supervision.{ProfessionalBody, Supervision}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.supervision.penalised_by_professional

import scala.concurrent.Future

class PenalisedByProfessionalController @Inject()(
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val authConnector: AuthConnector = AMLSAuthConnector
                                                 ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Supervision](Supervision.key) map {
        response =>
          val form = (for {
            supervision <- response
            professionalBody <- supervision.professionalBody
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
            supervision <- dataCacheConnector.fetch[Supervision](Supervision.key)
            _ <- dataCacheConnector.save[Supervision](Supervision.key,
              supervision.professionalBody(data))
          } yield Redirect(routes.SummaryController.get())
      }
  }
}