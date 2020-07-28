/*
 * Copyright 2020 HM Revenue & Customs
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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.renewal.{FXTransactionsInLast12Months, Renewal}
import play.api.mvc.{MessagesControllerComponents, Result}
import services.RenewalService
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.renewal.fx_transaction_in_last_12_months

import scala.concurrent.Future

class FXTransactionsInLast12MonthsController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                       val authAction: AuthAction,
                                                       val ds: CommonPlayDependencies,
                                                       val renewalService: RenewalService,
                                                       val cc: MessagesControllerComponents,
                                                       fx_transaction_in_last_12_months: fx_transaction_in_last_12_months) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Renewal](request.credId, Renewal.key) map {
          response =>
            val form: Form2[FXTransactionsInLast12Months] = (for {
              renewal <- response
              transactions <- renewal.fxTransactionsInLast12Months
            } yield Form2[FXTransactionsInLast12Months](transactions)).getOrElse(EmptyForm)
            Ok(fx_transaction_in_last_12_months(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request => {
        Form2[FXTransactionsInLast12Months](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(fx_transaction_in_last_12_months(f, edit)))
          case ValidForm(_, data) =>
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
        }
      }
  }

  private def standardRouting(businessActivities: Set[BusinessActivity], edit: Boolean): Result =
    (businessActivities, edit) match {
      case (x, false) if x.contains(HighValueDealing) || x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideIsUKController.get())
      case _ => Redirect(routes.SummaryController.get())
    }
}
