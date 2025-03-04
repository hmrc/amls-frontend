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

package controllers.businessdetails

import com.google.inject.Inject
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessdetails.PreviouslyRegisteredFormProvider
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.businessdetails.PreviouslyRegisteredService
import utils.AuthAction
import views.html.businessdetails.PreviouslyRegisteredView

import scala.concurrent.Future

class PreviouslyRegisteredController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  service: PreviouslyRegisteredService,
  formProvider: PreviouslyRegisteredFormProvider,
  view: PreviouslyRegisteredView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    service.getPreviouslyRegistered(request.credId).map { optPreviouslyRegistered =>
      Ok(view(optPreviouslyRegistered.fold(formProvider())(formProvider().fill), edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          service.updatePreviouslyRegistered(request.credId, data).map {
            _.fold(Redirect(routes.ConfirmRegisteredOfficeController.get(edit))) { _ =>
              getRouting(edit)
            }
          }
      )
  }

  private def getRouting(edit: Boolean): Result =
    if (edit) {
      Redirect(routes.SummaryController.get)
    } else {
      Redirect(routes.ActivityStartDateController.get(edit))
    }
}
