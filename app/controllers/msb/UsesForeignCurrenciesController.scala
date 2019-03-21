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
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.{CurrencyExchange, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness._
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.ControllerHelper

import scala.concurrent.Future

class UsesForeignCurrenciesController @Inject()(val authConnector: AuthConnector,
                                                implicit val dataCacheConnector: DataCacheConnector,
                                                implicit val statusService: StatusService,
                                                implicit val serviceFlow: ServiceFlow
                                          ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      ControllerHelper.allowedToEdit(MsbActivity, Some(CurrencyExchange)) flatMap {
        case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
          response =>
            val form: Form2[UsesForeignCurrencies] = (for {
              msb <- response
              currencies <- msb.whichCurrencies
              usesForeign <- currencies.usesForeignCurrencies
            } yield Form2[UsesForeignCurrencies](usesForeign)).getOrElse(EmptyForm)

            Ok(views.html.msb.uses_foreign_currencies(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      val foo = Form2[UsesForeignCurrencies](request.body)
      foo match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.uses_foreign_currencies(f, edit)))
        case ValidForm(_, data: UsesForeignCurrencies) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              updateCurrencies(msb, data))
          } yield
          edit match {
            case true => Redirect(routes.SummaryController.get())
            case _ => Redirect(routes.MoneySourcesController.get())
          }
      }
    }
  }

  def updateCurrencies(oldMsb: Option[MoneyServiceBusiness], usesForeignCurrencies: UsesForeignCurrencies): Option[MoneyServiceBusiness] = {
    oldMsb match {
      case Some(msb) => {
        msb.whichCurrencies match {
          case Some(w) => Some(msb.whichCurrencies(w.usesForeignCurrencies(usesForeignCurrencies)))
          case _ => None
        }
      }
      case _ => None
    }
  }
}
