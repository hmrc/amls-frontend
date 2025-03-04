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
import forms.renewal.TotalThroughputFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.businessmatching._
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.TotalThroughputView

import javax.inject.Inject
import scala.concurrent.Future

class TotalThroughputController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  renewals: RenewalService,
  dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: TotalThroughputFormProvider,
  view: TotalThroughputView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    val maybeResult = for {
      renewal    <- OptionT(renewals.getRenewal(request.credId))
      throughput <- OptionT.fromOption[Future](renewal.totalThroughput)
    } yield Ok(view(formProvider().fill(throughput), edit))

    maybeResult getOrElse Ok(view(formProvider(), edit))
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        value =>
          dataCacheConnector.fetchAll(request.credId) flatMap { optMap =>
            val result = for {
              cacheMap   <- optMap
              renewal    <- cacheMap.getEntry[Renewal](Renewal.key)
              bm         <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
              services   <- bm.msbServices
              activities <- bm.activities
            } yield renewals.updateRenewal(request.credId, renewal.totalThroughput(value)) map { _ =>
              if (!edit) {
                standardRouting(services.msbServices, activities.businessActivities)
              } else {
                Redirect(routes.SummaryController.get)
              }
            }
            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  private def standardRouting(
    services: Set[BusinessMatchingMsbService],
    businessActivities: Set[BusinessActivity]
  ): Result =
    if (services.contains(TransmittingMoney)) { Redirect(routes.TransactionsInLast12MonthsController.get()) }
    else if (services.contains(CurrencyExchange)) { Redirect(routes.CETransactionsInLast12MonthsController.get()) }
    else if (services.contains(ForeignExchange)) { Redirect(routes.FXTransactionsInLast12MonthsController.get()) }
    else if (businessActivities.contains(HighValueDealing) || businessActivities.contains(AccountancyServices)) {
      Redirect(routes.CustomersOutsideIsUKController.get())
    } else {
      Redirect(routes.SummaryController.get)
    }
}
