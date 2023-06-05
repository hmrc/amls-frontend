/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.msb.BranchesOrAgentsWhichCountriesFormProvider
import models.moneyservicebusiness.{BranchesOrAgents, BranchesOrAgentsHasCountries, MoneyServiceBusiness}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AutoCompleteService
import utils.AuthAction
import views.html.msb.BranchesOrAgentsWhichCountriesView

import javax.inject.Inject
import scala.concurrent.Future

class BranchesOrAgentsWhichCountriesController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                         authAction: AuthAction,
                                                         val ds: CommonPlayDependencies,
                                                         val autoCompleteService: AutoCompleteService,
                                                         val cc: MessagesControllerComponents,
                                                         formProvider: BranchesOrAgentsWhichCountriesFormProvider,
                                                         view: BranchesOrAgentsWhichCountriesView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
        response =>
          val form = (for {
            msb <- response
            boa <- msb.branchesOrAgents
            branches <- boa.branches
          } yield formProvider().fill(branches)).getOrElse(formProvider())
          Ok(view(form, edit, autoCompleteService.formOptions))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit, autoCompleteService.formOptions))),
        data =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
              msb.branchesOrAgents(BranchesOrAgents.update(msb.branchesOrAgents.getOrElse(BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)), data)))
          } yield if (edit) {
            Redirect(routes.SummaryController.get)
          } else {
            Redirect(routes.IdentifyLinkedTransactionsController.get())
          }
      )
  }
}