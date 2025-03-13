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
import forms.renewal.TransactionsInLast12MonthsFormProvider
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.businessmatching._
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.TransactionsInLast12MonthsView

import javax.inject.Inject
import scala.concurrent.Future

class TransactionsInLast12MonthsController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  renewalService: RenewalService,
  val cc: MessagesControllerComponents,
  formProvider: TransactionsInLast12MonthsFormProvider,
  view: TransactionsInLast12MonthsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      renewal   <- OptionT(renewalService.getRenewal(request.credId))
      transfers <- OptionT.fromOption[Future](renewal.transactionsInLast12Months)
    } yield Ok(view(formProvider().fill(transfers), edit))) getOrElse Ok(view(formProvider(), edit))
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        value =>
          dataCacheConnector.fetchAll(request.credId) flatMap { optMap =>
            (for {
              cacheMap <- optMap
              renewal  <- cacheMap.getEntry[Renewal](Renewal.key)
              bm       <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
              services <- bm.msbServices
            } yield renewalService.updateRenewal(request.credId, renewal.transactionsInLast12Months(value)) map { _ =>
              redirectTo(services.msbServices, edit)
            }) getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  private def redirectTo(services: Set[BusinessMatchingMsbService], edit: Boolean) =
    if (edit) {
      Redirect(routes.SummaryController.get)
    } else if (services contains TransmittingMoney) {
      Redirect(routes.SendMoneyToOtherCountryController.get())
    } else {
      Redirect(routes.SummaryController.get)
    }

}
