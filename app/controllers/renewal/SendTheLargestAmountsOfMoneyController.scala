/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{SendTheLargestAmountsOfMoney, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.send_largest_amounts_of_money

import scala.concurrent.Future

@Singleton
class SendTheLargestAmountsOfMoneyController @Inject()(
                                                        val dataCacheConnector: DataCacheConnector,
                                                        val authConnector: AuthConnector,
                                                        val renewalService: RenewalService
                                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Renewal](Renewal.key) map {
        response =>
          val form: Form2[SendTheLargestAmountsOfMoney] = (for {
            renewal <- response
            amount <- renewal.sendTheLargestAmountsOfMoney
          } yield Form2[SendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
          Ok(send_largest_amounts_of_money(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[SendTheLargestAmountsOfMoney](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(send_largest_amounts_of_money(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- renewalService.getRenewal
            _ <- renewalService.updateRenewal(renewal.sendTheLargestAmountsOfMoney(data))
          } yield redirectTo(edit, renewal)
      }
  }

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
