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
import forms.renewal.PercentageFormProvider
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.PercentageView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PercentageOfCashPaymentOver15000Controller @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val cc: MessagesControllerComponents,
  formProvider: PercentageFormProvider,
  view: PercentageView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Renewal](request.credId, Renewal.key) map { response =>
      val form = (for {
        renewal                          <- response
        percentageOfCashPaymentOver15000 <- renewal.percentageOfCashPaymentOver15000
      } yield formProvider().fill(percentageOfCashPaymentOver15000)).getOrElse(formProvider())
      Ok(view(form, edit))
    }

  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            renewal <- renewalService.getRenewal(request.credId)
            _       <- renewalService.updateRenewal(request.credId, renewal.percentageOfCashPaymentOver15000(data))
          } yield redirectDependingOnEdit(edit)
      )
  }

  private def redirectDependingOnEdit(edit: Boolean) = if (edit) {
    Redirect(routes.SummaryController.get)
  } else {
    Redirect(routes.CashPaymentsCustomersNotMetController.get(edit))
  }

}
