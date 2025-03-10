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

package controllers.asp

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.asp.OtherBusinessTaxMattersFormProvider
import models.asp.Asp
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.asp.OtherBusinessTaxMattersView

import javax.inject.Inject
import scala.concurrent.Future

class OtherBusinessTaxMattersController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: OtherBusinessTaxMattersFormProvider,
  view: OtherBusinessTaxMattersView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Asp](request.credId, Asp.key) map { response =>
      val form = (for {
        asp      <- response
        otherTax <- asp.otherBusinessTaxMatters
      } yield formProvider().fill(otherTax)).getOrElse(formProvider())
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
            asp <- dataCacheConnector.fetch[Asp](request.credId, Asp.key)
            _   <- dataCacheConnector.save[Asp](request.credId, Asp.key, asp.otherBusinessTaxMatters(data))
          } yield Redirect(routes.SummaryController.get)
      )
  }

}
