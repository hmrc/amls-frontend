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
import models.tcsp.Tcsp
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.tcsp._

import scala.concurrent.Future

class ServicesOfAnotherTCSPController @Inject()(
                                                 val authAction: AuthAction,
                                                 val ds: CommonPlayDependencies,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val cc: MessagesControllerComponents,
                                                 services_of_another_tcsp: services_of_another_tcsp) extends AmlsBaseController(ds, cc) {

  val NAME = "servicesOfAnotherTCSP"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(NAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(NAME, "error.required.tcsp.services.another.tcsp")

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>

      dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key) map {
        response =>
          val form: Form2[Boolean] = (for {
            tcsp <- response
            model <- tcsp.doesServicesOfAnotherTCSP
          } yield Form2[Boolean](model)) getOrElse EmptyForm
          Ok(services_of_another_tcsp(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>

      Form2[Boolean](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(services_of_another_tcsp(f, edit)))
        case ValidForm(_, data) =>
          for {
            tcsp <- dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key)
            _ <- dataCacheConnector.save[Tcsp](request.credId, Tcsp.key, {
              (data, tcsp.flatMap(t => t.doesServicesOfAnotherTCSP).contains(true)) match {
                case (false, true) => tcsp.doesServicesOfAnotherTCSP(data).copy(servicesOfAnotherTCSP = None)
                case _ => tcsp.doesServicesOfAnotherTCSP(data)
              }
            })
          } yield redirectTo(data, edit, tcsp)
      }
  }

  def redirectTo(data: Boolean, edit: Boolean, tcsp: Tcsp) = {
    (data, edit, tcsp.servicesOfAnotherTCSP.isDefined) match {
      case (true, _, false) => Redirect(routes.AnotherTCSPSupervisionController.get(edit))
      case _ => Redirect(routes.SummaryController.get())
    }
  }
}