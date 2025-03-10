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

package controllers.msb

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.msb.SendLargestAmountsFormProvider
import models.moneyservicebusiness.MoneyServiceBusiness
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.businessmatching.ServiceFlow
import services.{AutoCompleteService, StatusService}
import utils.AuthAction
import views.html.msb.SendLargestAmountsOfMoneyView

import javax.inject.Inject
import scala.concurrent.Future

class SendTheLargestAmountsOfMoneyController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val cacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: SendLargestAmountsFormProvider,
  view: SendLargestAmountsOfMoneyView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    cacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb    <- response
        amount <- msb.sendTheLargestAmountsOfMoney
      } yield amount).fold(formProvider())(formProvider().fill)
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
            msb <-
              cacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _   <- cacheConnector.save[MoneyServiceBusiness](
                     request.credId,
                     MoneyServiceBusiness.key,
                     msb.sendTheLargestAmountsOfMoney(Some(data))
                   )
          } yield Redirect(routes.MostTransactionsController.get(edit))
      )
  }
}
