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

import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.FXTransactionsInLast12MonthsFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.FXTransactionsInLast12MonthsView

import javax.inject.Inject
import scala.concurrent.Future

class FXTransactionsInLast12MonthsController @Inject()(val authAction: AuthAction,
                                                       val ds: CommonPlayDependencies,
                                                       val renewalService: RenewalService,
                                                       val cc: MessagesControllerComponents,
                                                       formProvider: FXTransactionsInLast12MonthsFormProvider,
                                                       view: FXTransactionsInLast12MonthsView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      for {
        renewal <- renewalService.getRenewal(request.credId)
        formOpt = renewal.fxTransactionsInLast12Months.map(formProvider().fill)
      } yield {
        Ok(view(formOpt.getOrElse(formProvider()), edit))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request => {
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          renewalService.fetchAndUpdateRenewal(
            request.credId,
            _.fxTransactionsInLast12Months(data)
          ) map {
            case Left(error) => InternalServerError(error)
            case Right(cacheMap) => standardRouting(
              cacheMap.getEntry[BusinessMatching](BusinessMatching.key).flatMap(_.activities.map(_.businessActivities)),
              edit
            )
          }
      )
    }
  }

  private def standardRouting(businessActivities: Option[Set[BusinessActivity]], edit: Boolean): Result =
    (businessActivities, edit) match {
      case (x, false) if x.fold(false)(y => y.contains(HighValueDealing) || y.contains(AccountancyServices)) =>
        Redirect(routes.CustomersOutsideIsUKController.get())
      case _ => Redirect(routes.SummaryController.get)
    }
}
