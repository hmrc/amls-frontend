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
import forms.renewal.UsesForeignCurrenciesFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching.BusinessMatchingMsbService.ForeignExchange
import models.businessmatching._
import models.renewal._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.UsesForeignCurrenciesView

import javax.inject.Inject
import scala.concurrent.Future

class UsesForeignCurrenciesController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  renewalService: RenewalService,
  dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: UsesForeignCurrenciesFormProvider,
  view: UsesForeignCurrenciesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    val block = for {
      renewal         <- OptionT(renewalService.getRenewal(request.credId))
      whichCurrencies <- OptionT.fromOption[Future](renewal.whichCurrencies)
      ufc             <- OptionT.fromOption[Future](whichCurrencies.usesForeignCurrencies)
    } yield Ok(view(formProvider().fill(ufc), edit))

    block getOrElse Ok(view(formProvider(), edit))
  }

  def post(edit: Boolean = false) = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap { optMap =>
            val result = for {
              cacheMap <- optMap
              renewal  <- cacheMap.getEntry[Renewal](Renewal.key)
              bm       <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
              ba       <- bm.activities
              services <- bm.msbServices
            } yield renewalService.updateRenewal(request.credId, updateCurrencies(renewal, data)) map { _ =>
              data match {
                case UsesForeignCurrenciesYes => Redirect(routes.MoneySourcesController.get(edit))
                case _                        => routing(ba.businessActivities, services.msbServices, edit)
              }
            }
            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  def updateCurrencies(oldRenewal: Renewal, usesForeignCurrencies: UsesForeignCurrencies) =
    oldRenewal.whichCurrencies match {
      case Some(wc) if usesForeignCurrencies.equals(UsesForeignCurrenciesYes) =>
        val newWc = wc.usesForeignCurrencies(usesForeignCurrencies)
        oldRenewal.whichCurrencies(newWc)
      case Some(wc) if usesForeignCurrencies.equals(UsesForeignCurrenciesNo)  =>
        val newWc = wc.usesForeignCurrencies(usesForeignCurrencies).moneySources(MoneySources())
        oldRenewal.whichCurrencies(newWc)
      case _                                                                  => oldRenewal
    }

  def routing(services: Set[BusinessActivity], msbServices: Set[BusinessMatchingMsbService], edit: Boolean) =
    if (msbServices.contains(ForeignExchange) && !edit) {
      Redirect(routes.FXTransactionsInLast12MonthsController.get(edit))
    } else if ((services.contains(HighValueDealing) || services.contains(AccountancyServices)) && !edit) {
      Redirect(routes.CustomersOutsideUKController.get(edit))
    } else {
      Redirect(routes.SummaryController.get)
    }
}
