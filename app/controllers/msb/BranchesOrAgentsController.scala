/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.moneyservicebusiness.{BranchesOrAgentsHasCountries, BranchesOrAgents, MoneyServiceBusiness}
import play.api.mvc.Call
import services.AutoCompleteService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class BranchesOrAgentsController @Inject() (val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector,
                                            val autoCompleteService: AutoCompleteService
                                           ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form = (for {
            msb <- response
            branches <- msb.branchesOrAgents
          } yield Form2[BranchesOrAgentsHasCountries](branches.hasCountries)).getOrElse(EmptyForm)
          Ok(views.html.msb.branches_or_agents(form, edit))
      }
  }


  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      Form2[BranchesOrAgentsHasCountries](request.body) match {

        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.branches_or_agents(f, edit)))

        case ValidForm(_, data) => {
          for {
            msb <-  dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            boa: BranchesOrAgents <- Future(msb.flatMap((m:MoneyServiceBusiness) => m.branchesOrAgents)
              .map((boa:BranchesOrAgents) => BranchesOrAgents.update(boa, data))
              .getOrElse(BranchesOrAgents(data, None)))
            _ <- dataCacheConnector.save(MoneyServiceBusiness.key, msb.branchesOrAgents(boa))
          } yield Redirect(getNextPage(data, edit))
        }
    }
  }

  private def getNextPage(data: BranchesOrAgentsHasCountries, edit: Boolean): Call =
     (data, edit) match {
      case (BranchesOrAgentsHasCountries(false), false) =>
        routes.IdentifyLinkedTransactionsController.get()
      case (BranchesOrAgentsHasCountries(false), true) =>
        routes.SummaryController.get()
      case (BranchesOrAgentsHasCountries(true), _) =>
        routes.BranchesOrAgentsWhichCountriesController.get(edit)
    }
}
