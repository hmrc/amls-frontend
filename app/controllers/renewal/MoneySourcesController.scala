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
import forms.renewal.MoneySourcesFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching.BusinessMatchingMsbService.ForeignExchange
import models.businessmatching._
import models.renewal.{MoneySources, Renewal}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.MoneySourcesView

import javax.inject.Inject
import scala.concurrent.Future

class MoneySourcesController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  renewalService: RenewalService,
  dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: MoneySourcesFormProvider,
  view: MoneySourcesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    val block = for {
      renewal         <- OptionT(renewalService.getRenewal(request.credId))
      whichCurrencies <- OptionT.fromOption[Future](renewal.whichCurrencies)
      ms              <- OptionT.fromOption[Future](whichCurrencies.moneySources)
    } yield Ok(view(formProvider().fill(ms), edit))

    block getOrElse Ok(view(formProvider(), edit))
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(view(formWithError, edit))),
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap { optMap =>
            val result = for {
              cacheMap   <- optMap
              renewal    <- cacheMap.getEntry[Renewal](Renewal.key)
              bm         <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
              services   <- bm.msbServices
              activities <- bm.activities
            } yield renewalService.updateRenewal(request.credId, updateMoneySources(renewal, data)) map { _ =>
              standardRouting(services.msbServices, activities.businessActivities, edit)
            }
            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  def updateMoneySources(oldRenewal: Renewal, moneySources: MoneySources): Renewal =
    oldRenewal match {
      case renewal: Renewal =>
        renewal.whichCurrencies match {
          case Some(w) => Some(renewal.whichCurrencies(w.moneySources(moneySources)))
          case _       => None
        }
      case _                => None
    }

  private def standardRouting(
    services: Set[BusinessMatchingMsbService],
    businessActivities: Set[BusinessActivity],
    edit: Boolean
  ): Result =
    (services, businessActivities, edit) match {
      case (x, _, false) if x.contains(ForeignExchange)                                     => Redirect(routes.FXTransactionsInLast12MonthsController.get())
      case (_, x, false) if x.contains(HighValueDealing) || x.contains(AccountancyServices) =>
        Redirect(routes.CustomersOutsideIsUKController.get())
      case _                                                                                => Redirect(routes.SummaryController.get)
    }
}
