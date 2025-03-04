/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.msb.CurrencyExchangesInNext12MonthsFormProvider
import models.moneyservicebusiness.MoneyServiceBusiness
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.msb.CurrencyExchangesInNext12MonthsView

import javax.inject.Inject
import scala.concurrent.Future

class CurrencyExchangesInNext12MonthsController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  formProvider: CurrencyExchangesInNext12MonthsFormProvider,
  view: CurrencyExchangesInNext12MonthsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb          <- response
        transactions <- msb.ceTransactionsInNext12Months
      } yield formProvider().fill(transactions)).getOrElse(formProvider())
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _   <- dataCacheConnector.save[MoneyServiceBusiness](
                     request.credId,
                     MoneyServiceBusiness.key,
                     msb.ceTransactionsInNext12Months(data)
                   )
          } yield edit match {
            case true if msb.whichCurrencies.isDefined =>
              Redirect(routes.SummaryController.get)
            case _                                     =>
              Redirect(routes.WhichCurrenciesController.get(edit))
          }
      )
  }
}
