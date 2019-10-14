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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.renewal.{Renewal, SendTheLargestAmountsOfMoney}
import play.api.mvc.MessagesControllerComponents
import services.{AutoCompleteService, RenewalService}
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.send_largest_amounts_of_money

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SendTheLargestAmountsOfMoneyController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                       val authAction: AuthAction,
                                                       val ds: CommonPlayDependencies,
                                                       val renewalService: RenewalService,
                                                       val autoCompleteService: AutoCompleteService,
                                                       val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Renewal](request.credId, Renewal.key) map {
        response =>
          val form: Form2[SendTheLargestAmountsOfMoney] = (for {
            renewal <- response
            amount <- renewal.sendTheLargestAmountsOfMoney
          } yield Form2[SendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
          Ok(send_largest_amounts_of_money(form, edit, autoCompleteService.getCountries))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[SendTheLargestAmountsOfMoney](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(send_largest_amounts_of_money(alignFormDataWithValidationErrors(f), edit, autoCompleteService.getCountries)))
        case ValidForm(_, data) =>
          for {
            renewal <- renewalService.getRenewal(request.credId)
            _ <- renewalService.updateRenewal(request.credId, renewal.sendTheLargestAmountsOfMoney(data))
          } yield redirectTo(edit, renewal)
      }
  }

  def alignFormDataWithValidationErrors(form: InvalidForm): InvalidForm =
    ControllerHelper.stripEmptyValuesFromFormWithArray(form, "largestAmountsOfMoney", index => index / 2)


  def redirectTo(edit:Boolean, renewal: Renewal) = edit match {
    case true if !mostTransactionsDataRequired(renewal)  => Redirect(routes.SummaryController.get())
    case _ => Redirect(routes.MostTransactionsController.get(edit))
  }

  private def mostTransactionsDataRequired(renewal: Renewal): Boolean = {
    (renewal.customersOutsideUK, renewal.mostTransactions) match {
      case (Some(_), None) => true
      case _ => false
    }
  }
}
