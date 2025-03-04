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
import forms.msb.WhichCurrenciesFormProvider

import javax.inject.Inject
import models.moneyservicebusiness._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CurrencyAutocompleteService, StatusService}
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.msb.WhichCurrenciesView

import scala.concurrent.Future

class WhichCurrenciesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  autocompleteService: CurrencyAutocompleteService,
  formProvider: WhichCurrenciesFormProvider,
  view: WhichCurrenciesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb        <- response
        currencies <- msb.whichCurrencies
      } yield currencies).fold(formProvider())(formProvider().fill)

      Ok(view(form, edit, autocompleteService.formOptions))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, autocompleteService.formOptions))),
        data =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _   <- dataCacheConnector
                     .save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key, updateCurrencies(msb, data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get)
            case _    => Redirect(routes.UsesForeignCurrenciesController.get())
          }
      )
  }

  def updateCurrencies(
    oldMsb: Option[MoneyServiceBusiness],
    newWhichCurrencies: WhichCurrencies
  ): Option[MoneyServiceBusiness] =
    oldMsb match {
      case Some(msb) =>
        msb.whichCurrencies match {
          case Some(w) => Some(msb.whichCurrencies(w.currencies(newWhichCurrencies.currencies)))
          case _       => Some(msb.whichCurrencies(WhichCurrencies(newWhichCurrencies.currencies)))
        }
      case _         => None
    }
}
