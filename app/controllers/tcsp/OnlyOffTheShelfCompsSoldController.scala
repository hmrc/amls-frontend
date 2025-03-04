/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.tcsp.OnlyOffTheShelfCompsSoldFormProvider
import models.tcsp.TcspTypes.CompanyFormationAgent

import javax.inject.Inject
import models.tcsp._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.AuthAction
import views.html.tcsp.OnlyOffTheShelfCompsSoldView

import scala.concurrent.Future

class OnlyOffTheShelfCompsSoldController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: OnlyOffTheShelfCompsSoldFormProvider,
  view: OnlyOffTheShelfCompsSoldView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key) map { response =>
      val form = (for {
        tcsp  <- response
        model <- tcsp.onlyOffTheShelfCompsSold
      } yield formProvider().fill(model)) getOrElse formProvider()
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            tcsp <- dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key)
            _    <- dataCacheConnector.save[Tcsp](request.credId, Tcsp.key, tcsp.onlyOffTheShelfCompsSold(data))
          } yield redirectTo(edit, tcsp)
      )
  }

  def redirectTo(edit: Boolean, tcsp: Tcsp): Result =
    (edit, tcsp.tcspTypes.map(t => t.serviceProviders.contains(CompanyFormationAgent))) match {
      case (_, Some(true)) => Redirect(routes.ComplexCorpStructureCreationController.get(edit))
      case _               => Redirect(routes.SummaryController.get())
    }
}
