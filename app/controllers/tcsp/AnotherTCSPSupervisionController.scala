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

package controllers.tcsp

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.tcsp.{ServicesOfAnotherTCSP, Tcsp}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class AnotherTCSPSupervisionController @Inject()(val authAction: AuthAction,
                                                 val ds: CommonPlayDependencies,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key) map {
        response =>
          val form: Form2[ServicesOfAnotherTCSP] = (for {
            tcsp <- response
            model <- tcsp.servicesOfAnotherTCSP
          } yield Form2[ServicesOfAnotherTCSP](model)) getOrElse EmptyForm
          Ok(views.html.tcsp.another_tcsp_supervision(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[ServicesOfAnotherTCSP](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tcsp.another_tcsp_supervision(f, edit)))
        case ValidForm(_, data) =>

          for {
            tcsp <- dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key)
            _ <- dataCacheConnector.save[Tcsp](request.credId, Tcsp.key, tcsp.servicesOfAnotherTCSP(data))
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}