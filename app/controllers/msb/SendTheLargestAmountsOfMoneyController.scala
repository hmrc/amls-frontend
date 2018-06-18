/*
 * Copyright 2018 HM Revenue & Customs
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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.{TransmittingMoney, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness.{MoneyServiceBusiness, SendTheLargestAmountsOfMoney}
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.msb.send_largest_amounts_of_money

import scala.concurrent.Future

class SendTheLargestAmountsOfMoneyController @Inject()(val authConnector: AuthConnector = AMLSAuthConnector,
                                                       implicit val cacheConnector: DataCacheConnector,
                                                       implicit val statusService: StatusService,
                                                       implicit val serviceFlow: ServiceFlow
                                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit(MsbActivity, Some(TransmittingMoney)) flatMap {
        case true => cacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
          response =>
            val form: Form2[SendTheLargestAmountsOfMoney] = (for {
              msb <- response
              amount <- msb.sendTheLargestAmountsOfMoney
            } yield Form2[SendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
            Ok(send_largest_amounts_of_money(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[SendTheLargestAmountsOfMoney](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(send_largest_amounts_of_money(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <-
            cacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- cacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.sendTheLargestAmountsOfMoney(data)
            )
          } yield edit match {
            case true if msb.mostTransactions.isDefined =>
              Redirect(routes.SummaryController.get())
            case _ =>
              Redirect(routes.MostTransactionsController.get(edit))
          }
      }
  }
}
