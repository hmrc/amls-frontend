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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.hvd.ExpectToReceiveFormProvider
import models.hvd.Hvd
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.hvd.ExpectToReceiveView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ExpectToReceiveCashPaymentsController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val cacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  formProvider: ExpectToReceiveFormProvider,
  view: ExpectToReceiveView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    cacheConnector.fetch[Hvd](request.credId, Hvd.key) map { response =>
      val form = (for {
        hvd             <- response
        receivePayments <- hvd.cashPaymentMethods
      } yield formProvider().fill(receivePayments)).getOrElse(formProvider())

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
            hvd <- cacheConnector.fetch[Hvd](request.credId, Hvd.key)
            _   <- cacheConnector.save[Hvd](request.credId, Hvd.key, hvd.cashPaymentMethods(data))
          } yield
            if (edit) {
              Redirect(routes.SummaryController.get)
            } else {
              Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
            }
      )
  }
}
