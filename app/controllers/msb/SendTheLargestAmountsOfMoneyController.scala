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

package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{SendTheLargestAmountsOfMoney, MoneyServiceBusiness}
import services.StatusService
import utils.ControllerHelper
import views.html.msb.send_largest_amounts_of_money

import scala.concurrent.Future

trait SendTheLargestAmountsOfMoneyController extends BaseController {
  val dataCacheConnector: DataCacheConnector
  implicit val statusService: StatusService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
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
            dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
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

object SendTheLargestAmountsOfMoneyController extends SendTheLargestAmountsOfMoneyController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService: StatusService = StatusService
}
