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

package controllers.businessactivities

import com.google.inject.Inject
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.EmployeeCountFormProvider
import models.businessactivities.EmployeeCount
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.businessactivities.HowManyEmployeesService
import utils.AuthAction
import views.html.businessactivities.BusinessEmployeesCountView

import scala.concurrent.Future

class HowManyEmployeesController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  service: HowManyEmployeesService,
  formProvider: EmployeeCountFormProvider,
  view: BusinessEmployeesCountView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    service.getEmployeeCount(request.credId) map { countOpt =>
      val form = countOpt.fold(formProvider())(count => formProvider().fill(EmployeeCount(count)))
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          service.updateEmployeeCount(request.credId, data) map { _ =>
            redirect(edit)
          }
      )
  }

  private def redirect(edit: Boolean): Result = if (edit) {
    Redirect(routes.SummaryController.get)
  } else {
    Redirect(routes.TransactionRecordController.get())
  }
}
