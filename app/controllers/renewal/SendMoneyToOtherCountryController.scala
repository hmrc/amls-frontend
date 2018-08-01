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

package controllers.renewal

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.renewal.{Renewal, SendMoneyToOtherCountry}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.send_money_to_other_country

import scala.concurrent.Future

class SendMoneyToOtherCountryController @Inject()(
                                                   val authConnector: AuthConnector,
                                                   val dataCacheConnector: DataCacheConnector,
                                                   renewalService: RenewalService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          renewal <- OptionT(renewalService.getRenewal)
          otherCountry <- OptionT.fromOption[Future](renewal.sendMoneyToOtherCountry)
        } yield {
          Ok(send_money_to_other_country(Form2[SendMoneyToOtherCountry](otherCountry), edit))
        }) getOrElse Ok(send_money_to_other_country(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[SendMoneyToOtherCountry](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(send_money_to_other_country(f, edit)))
          case ValidForm(_, model) =>
            dataCacheConnector.fetchAll flatMap {
              optMap =>
                (for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  services <- bm.msbServices
                  activities <- bm.activities
                } yield {

                  renewalService.updateRenewal(model.money match {
                    case false => renewal.sendMoneyToOtherCountry(model).copy(
                      mostTransactions = None, sendTheLargestAmountsOfMoney = None)
                    case true => renewal.sendMoneyToOtherCountry(model)
                  }) map { _ =>
                    redirect(
                      model.money,
                      services.msbServices,
                      activities.businessActivities,
                      edit
                    )
                  }
                }) getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  private def redirect(sendMoneyToOtherCountry: Boolean, services: Set[BusinessMatchingMsbService], activities: Set[BusinessActivity], edit: Boolean) = {
    (sendMoneyToOtherCountry, edit, services, activities) match {
      case (true, _, _, _) => Redirect(routes.SendTheLargestAmountsOfMoneyController.get(edit))
      case (_, false, x, _) if x.contains(CurrencyExchange) =>  Redirect(routes.CETransactionsInLast12MonthsController.get())
      case (_, false, _, x) if x.contains(HighValueDealing) && !x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideUKController.get())
      case (_, false, _, x) if x.contains(HighValueDealing) => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
      case _ => Redirect(routes.SummaryController.get())
    }
  }

}
