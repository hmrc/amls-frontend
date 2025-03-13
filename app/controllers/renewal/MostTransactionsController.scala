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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.MostTransactionsFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange}
import models.businessmatching._
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AutoCompleteService, RenewalService}
import utils.AuthAction
import views.html.renewal.MostTransactionsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class MostTransactionsController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cache: DataCacheConnector,
  val renewalService: RenewalService,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: MostTransactionsFormProvider,
  view: MostTransactionsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    cache.fetch[Renewal](request.credId, Renewal.key) map { response =>
      val form = (for {
        msb          <- response
        transactions <- msb.mostTransactions
      } yield formProvider().fill(transactions)).getOrElse(formProvider())
      Ok(view(form, edit, autoCompleteService.formOptions))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, autoCompleteService.formOptions))),
        data =>
          cache.fetchAll(request.credId).flatMap { optMap =>
            val result = for {
              cacheMap <- optMap
              renewal  <- cacheMap.getEntry[Renewal](Renewal.key)
              bm       <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
              ba       <- bm.activities
              services <- bm.msbServices
            } yield renewalService.updateRenewal(request.credId, renewal.mostTransactions(data)) map { _ =>
              if (!edit) {
                redirectTo(services.msbServices, ba.businessActivities)
              } else {
                Redirect(routes.SummaryController.get)
              }
            }
            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  private def redirectTo(services: Set[BusinessMatchingMsbService], businessActivities: Set[BusinessActivity]): Result =
    (services, businessActivities) match {
      case (x, _) if x.contains(CurrencyExchange)                                    => Redirect(routes.CETransactionsInLast12MonthsController.get())
      case (x, _) if x.contains(ForeignExchange)                                     => Redirect(routes.FXTransactionsInLast12MonthsController.get())
      case (_, x) if x.contains(HighValueDealing) && x.contains(AccountancyServices) =>
        Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
      case (_, x) if x.contains(HighValueDealing) || x.contains(AccountancyServices) =>
        Redirect(routes.CustomersOutsideIsUKController.get())
      case _                                                                         => Redirect(routes.SummaryController.get)
    }
}
