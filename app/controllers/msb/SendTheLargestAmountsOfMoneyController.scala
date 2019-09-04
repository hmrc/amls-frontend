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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.moneyservicebusiness.{MoneyServiceBusiness, SendTheLargestAmountsOfMoney}
import services.businessmatching.ServiceFlow
import services.{AutoCompleteService, StatusService}
import utils.AuthAction
import views.html.msb.send_largest_amounts_of_money

import scala.concurrent.Future

class SendTheLargestAmountsOfMoneyController @Inject()(authAction: AuthAction, val ds: CommonPlayDependencies,
                                                       implicit val cacheConnector: DataCacheConnector,
                                                       implicit val statusService: StatusService,
                                                       implicit val serviceFlow: ServiceFlow,
                                                       val autoCompleteService: AutoCompleteService
                                                      ) extends AmlsBaseController(ds) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      cacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[SendTheLargestAmountsOfMoney] = (for {
            msb <- response
            amount <- msb.sendTheLargestAmountsOfMoney
          } yield Form2[SendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
          Ok(send_largest_amounts_of_money(form, edit, autoCompleteService.getCountries))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[SendTheLargestAmountsOfMoney](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(send_largest_amounts_of_money(f, edit, autoCompleteService.getCountries)))
        case ValidForm(_, data) =>
          for {
            msb <-
            cacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _ <- cacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
              msb.sendTheLargestAmountsOfMoney(Some(data))
            )
          } yield {
           Redirect(routes.MostTransactionsController.get(edit))
          }
      }
  }
}
