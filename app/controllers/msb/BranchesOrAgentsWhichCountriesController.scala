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

package controllers.msb

import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.msb.BranchesOrAgentsWhichCountriesFormProvider
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AutoCompleteService
import services.msb.BranchesOrAgentsWhichCountriesService
import utils.AuthAction
import views.html.msb.BranchesOrAgentsWhichCountriesView

import javax.inject.Inject
import scala.concurrent.Future

class BranchesOrAgentsWhichCountriesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  branchesOrAgentsWhichCountriesService: BranchesOrAgentsWhichCountriesService,
  formProvider: BranchesOrAgentsWhichCountriesFormProvider,
  view: BranchesOrAgentsWhichCountriesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    branchesOrAgentsWhichCountriesService.fetchBranchesOrAgents(request.credId) map { branchesOrAgents =>
      Ok(view(branchesOrAgents.fold(formProvider())(formProvider().fill), edit, autoCompleteService.formOptions))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, autoCompleteService.formOptions))),
        data => {
          val redirect = if (edit) {
            Redirect(routes.SummaryController.get)
          } else {
            Redirect(routes.IdentifyLinkedTransactionsController.get())
          }
          branchesOrAgentsWhichCountriesService.fetchAndSaveBranchesOrAgents(request.credId, data, redirect)
        }
      )
  }
}
