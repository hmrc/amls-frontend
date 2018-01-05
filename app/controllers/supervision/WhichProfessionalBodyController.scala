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
import models.supervision.{BusinessTypes, Supervision}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.supervision.which_professional_body

import scala.concurrent.Future

class WhichProfessionalBodyController @Inject()(
                                               val dataCacheConnector: DataCacheConnector,
                                               val authConnector: AuthConnector = AMLSAuthConnector
                                               ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[Supervision](Supervision.key) map { response =>

          val form = (for {
            supervision <- response
            businessTypes <- supervision.businessTypes
          } yield {
            Form2[BusinessTypes](businessTypes)
          }) getOrElse EmptyForm

          Ok(which_professional_body(form, edit))
        }
  }

  def post(edit: Boolean = false) = Authorised.async{
    implicit authContext =>
      implicit request =>
        Form2[BusinessTypes](request.body) match {
          case ValidForm(_, data) =>
            for {
              supervision <- dataCacheConnector.fetch[Supervision](Supervision.key)
              _ <- dataCacheConnector.save[Supervision](Supervision.key,supervision.businessTypes(Some(data)))
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
