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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.msb.BranchesOrAgentsFormProvider
import models.moneyservicebusiness.{BranchesOrAgents, BranchesOrAgentsHasCountries, MoneyServiceBusiness}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.AutoCompleteService
import utils.AuthAction
import views.html.msb.BranchesOrAgentsView

import javax.inject.Inject
import scala.concurrent.Future

class BranchesOrAgentsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: BranchesOrAgentsFormProvider,
  view: BranchesOrAgentsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb      <- response
        branches <- msb.branchesOrAgents
      } yield formProvider().fill(branches.hasCountries)).getOrElse(formProvider())
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
            msb                   <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            boa: BranchesOrAgents <- Future(
                                       msb
                                         .flatMap((m: MoneyServiceBusiness) => m.branchesOrAgents)
                                         .map((boa: BranchesOrAgents) => BranchesOrAgents.update(boa, data))
                                         .getOrElse(BranchesOrAgents(data, None))
                                     )
            _                     <- dataCacheConnector.save(request.credId, MoneyServiceBusiness.key, msb.branchesOrAgents(boa))
          } yield Redirect(getNextPage(data, edit))
      )
  }

  private def getNextPage(data: BranchesOrAgentsHasCountries, edit: Boolean): Call =
    (data, edit) match {
      case (BranchesOrAgentsHasCountries(false), false) =>
        routes.IdentifyLinkedTransactionsController.get()
      case (BranchesOrAgentsHasCountries(false), true)  =>
        routes.SummaryController.get
      case (BranchesOrAgentsHasCountries(true), _)      =>
        routes.BranchesOrAgentsWhichCountriesController.get(edit)
    }
}
