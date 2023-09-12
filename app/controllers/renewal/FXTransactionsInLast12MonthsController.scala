/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.renewal.FXTransactionsInLast12MonthsFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching._
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.FXTransactionsInLast12MonthsView

import javax.inject.Inject
import scala.concurrent.Future

class FXTransactionsInLast12MonthsController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                       val authAction: AuthAction,
                                                       val ds: CommonPlayDependencies,
                                                       val renewalService: RenewalService,
                                                       val cc: MessagesControllerComponents,
                                                       formProvider: FXTransactionsInLast12MonthsFormProvider,
                                                       view: FXTransactionsInLast12MonthsView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Renewal](request.credId, Renewal.key) map {
        response =>
          val form = (for {
            renewal <- response
            transactions <- renewal.fxTransactionsInLast12Months
          } yield formProvider().fill(transactions)).getOrElse(formProvider())
          Ok(view(form, edit))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request => {
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap {
            optMap =>
              val result = for {
                cacheMap <- optMap
                renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                activities <- bm.activities
              } yield {
                renewalService.updateRenewal(request.credId, renewal.fxTransactionsInLast12Months(data)) map { _ =>
                  standardRouting(activities.businessActivities, edit)
                }
              }
              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
    }
  }

  private def standardRouting(businessActivities: Set[BusinessActivity], edit: Boolean): Result =
    (businessActivities, edit) match {
      case (x, false) if x.contains(HighValueDealing) || x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideIsUKController.get())
      case _ => Redirect(routes.SummaryController.get)
    }
}
