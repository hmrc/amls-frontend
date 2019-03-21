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
import models.businessmatching.{CurrencyExchange, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness._
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

class WhichCurrenciesController @Inject() (val authConnector: AuthConnector,
                                           implicit val dataCacheConnector: DataCacheConnector,
                                           implicit val statusService: StatusService,
                                           implicit val serviceFlow: ServiceFlow
                                          ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      ControllerHelper.allowedToEdit(MsbActivity, Some(CurrencyExchange)) flatMap {
        case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form = (for {
            msb <- response
            currencies <- msb.whichCurrencies
          } yield Form2[WhichCurrencies](currencies)).getOrElse(EmptyForm)

          Ok(views.html.msb.which_currencies(form, edit))
      }
        case false => Future.successful(NotFound(notFoundView))
      }
    }
  }
  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[WhichCurrencies](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.which_currencies(f, edit)))
        case ValidForm(_, data: WhichCurrencies) =>
              for {
                msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
                _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
                  updateCurrencies(msb, data))
              } yield Redirect(routes.UsesForeignCurrenciesController.get())
//                  edit match {
//                    case true => Redirect(routes.SummaryController.get())
//                    case _ => Redirect(routes.UsesForeignCurrenciesController.get())
//                  }

      }
    }
  }

  def updateCurrencies(oldMsb: Option[MoneyServiceBusiness], newWhichCurrencies: WhichCurrencies): Option[MoneyServiceBusiness] = {
    oldMsb match {
      case Some(msb) => {
       msb.whichCurrencies match {
          case Some(w) => Some(msb.whichCurrencies(w.currencies(newWhichCurrencies.currencies)))
          case _ => Some(msb.whichCurrencies(WhichCurrencies(newWhichCurrencies.currencies, None, Some(MoneySources(None, None, None)))))
        }
      }
      case _ => None
    }

  }
}
