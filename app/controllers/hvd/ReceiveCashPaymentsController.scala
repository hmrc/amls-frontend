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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.hvd.Hvd
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction

import views.html.hvd.receiving

import scala.concurrent.Future

class ReceiveCashPaymentsController @Inject()(val authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              implicit val cacheConnector: DataCacheConnector,
                                              implicit val serviceFlow: ServiceFlow,
                                              implicit val statusService: StatusService,
                                              val cc: MessagesControllerComponents,
                                              receiving: receiving) extends AmlsBaseController(ds, cc) {

  val NAME = "receivePayments"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(NAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(NAME, "error.required.hvd.receive.cash.payments")

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        cacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
          response =>
            val form: Form2[Boolean] = (for {
              hvd <- response
              receivePayments <- hvd.receiveCashPayments
            } yield Form2[Boolean](receivePayments)).getOrElse(EmptyForm)
            Ok(receiving(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request => {
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(receiving(f, edit)))
          case ValidForm(_, data) => {
            for {
              hvd <- cacheConnector.fetch[Hvd](request.credId, Hvd.key)
              _ <- cacheConnector.save[Hvd](request.credId, Hvd.key, {
                (hvd.flatMap(h => h.receiveCashPayments).contains(true), data) match {
                  case (true, false) => hvd.receiveCashPayments(data).copy(cashPaymentMethods = None)
                  case _ => hvd.receiveCashPayments(data)
                }
              })
            } yield redirectTo(data, hvd, edit)
          }
        }
      }
  }

  def redirectTo(data: Boolean, hvd: Hvd, edit: Boolean) =
    (data, edit, hvd.cashPaymentMethods.isDefined) match {
      case (true, _, false) => Redirect(routes.ExpectToReceiveCashPaymentsController.get(edit))
      case (_, true, _)     => Redirect(routes.SummaryController.get())
      case _                => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get(edit))
    }
}
