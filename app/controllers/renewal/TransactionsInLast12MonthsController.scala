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

import javax.inject.Inject

import cats.data.OptionT
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.TransactionsInLast12Months
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.transactions_in_last_12_months
import cats.implicits._

import scala.concurrent.Future

class TransactionsInLast12MonthsController @Inject()(val authConnector: AuthConnector, renewalService: RenewalService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        val block = for {
          renewal <- OptionT(renewalService.getRenewal)
          transfers <- OptionT.fromOption[Future](renewal.transactionsInLast12Months)
        } yield {
          Ok(transactions_in_last_12_months(Form2[TransactionsInLast12Months](transfers), edit))
        }

        block getOrElse Ok(transactions_in_last_12_months(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        Form2[TransactionsInLast12Months](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(transactions_in_last_12_months(f, edit)))
          case ValidForm(_, model) =>
            val maybeResponse = for {
              renewal <- OptionT(renewalService.getRenewal)
              _ <- OptionT.liftF(renewalService.updateRenewal(renewal.transactionsInLast12Months(model)))
            } yield {
              Redirect(nextPageUrl(edit))
            }

            maybeResponse getOrElse Redirect(routes.SummaryController.get())
        }
  }

  private def nextPageUrl(edit: Boolean) = {
    if (edit) {
      routes.SummaryController.get()
    } else {
      routes.SendTheLargestAmountsOfMoneyController.get()
    }
  }
}
