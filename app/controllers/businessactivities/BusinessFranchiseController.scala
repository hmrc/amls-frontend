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
import forms.businessactivities.BusinessFranchiseFormProvider
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.businessactivities.BusinessFranchiseService
import utils.AuthAction
import views.html.businessactivities.BusinessFranchiseNameView

import scala.concurrent.Future

class BusinessFranchiseController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  service: BusinessFranchiseService,
  formProvider: BusinessFranchiseFormProvider,
  view: BusinessFranchiseNameView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    service.getBusinessFranchise(request.credId) map { franchiseOpt =>
      Ok(view(franchiseOpt.fold(formProvider())(formProvider().fill), edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data => service.updateBusinessFranchise(request.credId, data).map(_ => redirectTo(edit))
      )
  }

  private def redirectTo(edit: Boolean): Result = if (edit) {
    Redirect(routes.SummaryController.get)
  } else {
    Redirect(routes.EmployeeCountAMLSSupervisionController.get())
  }
}
