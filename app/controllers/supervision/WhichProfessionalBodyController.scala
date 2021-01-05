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

package controllers.supervision

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.supervision.{ProfessionalBodies, Supervision}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction

import views.html.supervision.which_professional_body

import scala.concurrent.Future

class WhichProfessionalBodyController @Inject()(
                                               val dataCacheConnector: DataCacheConnector,
                                               val authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val cc: MessagesControllerComponents,
                                               which_professional_body: which_professional_body) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map { response =>

          val form = (for {
            supervision <- response
            businessTypes <- supervision.professionalBodies
          } yield {
            Form2[ProfessionalBodies](businessTypes)
          }) getOrElse EmptyForm

          Ok(which_professional_body(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async{
      implicit request =>
        Form2[ProfessionalBodies](request.body) match {
          case ValidForm(_, data) =>
            for {
              supervision <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
              _ <- dataCacheConnector.save[Supervision](request.credId, Supervision.key,supervision.professionalBodies(Some(data)))
            } yield {
              if(edit){
                Redirect(routes.SummaryController.post())
              } else {
                Redirect(routes.PenalisedByProfessionalController.post(edit))
              }
            }
          case f:InvalidForm => Future.successful(BadRequest(which_professional_body(f, edit)))
        }
  }

}
