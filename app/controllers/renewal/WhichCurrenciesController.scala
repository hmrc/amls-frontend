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

package controllers.renewal

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.WhichCurrenciesFormProvider
import models.renewal.{Renewal, WhichCurrencies}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CurrencyAutocompleteService, RenewalService}
import utils.AuthAction
import views.html.renewal.WhichCurrenciesView

import javax.inject.Inject
import scala.concurrent.Future

class WhichCurrenciesController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  renewalService: RenewalService,
  dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  autocompleteService: CurrencyAutocompleteService,
  formProvider: WhichCurrenciesFormProvider,
  view: WhichCurrenciesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    val block = for {
      renewal         <- OptionT(renewalService.getRenewal(request.credId))
      whichCurrencies <- OptionT.fromOption[Future](renewal.whichCurrencies)
    } yield Ok(view(formProvider().fill(whichCurrencies), edit, autocompleteService.formOptions))

    block getOrElse Ok(view(formProvider(), edit, autocompleteService.formOptions))
  }

  def post(edit: Boolean = false) = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, autocompleteService.formOptions))),
        data =>
          dataCacheConnector.fetchAll(request.credId).flatMap { optMap =>
            val result = for {
              cacheMap <- optMap
              renewal  <- cacheMap.getEntry[Renewal](Renewal.key)
            } yield renewalService.updateRenewal(request.credId, updateWhichCurrencies(renewal, data)) map { _ =>
              if (edit) {
                Redirect(routes.SummaryController.get)
              } else {
                Redirect(routes.UsesForeignCurrenciesController.get())
              }
            }

            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  def updateWhichCurrencies(oldRenewal: Renewal, whichCurrencies: WhichCurrencies) =
    oldRenewal.whichCurrencies match {
      case Some(wc) =>
        val newWc = wc.currencies(whichCurrencies.currencies)
        oldRenewal.whichCurrencies(newWc)
      case None     =>
        val newWc = WhichCurrencies(whichCurrencies.currencies)
        oldRenewal.whichCurrencies(newWc)
    }
}
