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
import forms.renewal.SendLargestAmountsOfMoneyFormProvider
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AutoCompleteService, RenewalService}
import utils.AuthAction
import views.html.renewal.SendLargestAmountsOfMoneyView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SendTheLargestAmountsOfMoneyController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: SendLargestAmountsOfMoneyFormProvider,
  view: SendLargestAmountsOfMoneyView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Renewal](request.credId, Renewal.key) map { response =>
      val form = (for {
        renewal <- response
        amount  <- renewal.sendTheLargestAmountsOfMoney
      } yield formProvider().fill(amount)).getOrElse(formProvider())
      Ok(view(form, edit, autoCompleteService.formOptions))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, autoCompleteService.formOptions))),
        data =>
          for {
            renewal <- renewalService.getRenewal(request.credId)
            _       <- renewalService.updateRenewal(request.credId, renewal.sendTheLargestAmountsOfMoney(data))
          } yield redirectTo(edit, renewal)
      )
  }

  def redirectTo(edit: Boolean, renewal: Renewal) = edit match {
    case true if !mostTransactionsDataRequired(renewal) => Redirect(routes.SummaryController.get)
    case _                                              => Redirect(routes.MostTransactionsController.get(edit))
  }

  private def mostTransactionsDataRequired(renewal: Renewal): Boolean =
    (renewal.customersOutsideUK, renewal.mostTransactions) match {
      case (Some(_), None) => true
      case _               => false
    }
}
