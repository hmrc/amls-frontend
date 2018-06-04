/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.HighValueDealing
import models.hvd.Hvd
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.hvd.receiving

import scala.concurrent.Future

class ReceiveCashPaymentsController @Inject() (val cacheConnector: DataCacheConnector,
                                               implicit val serviceFlow: ServiceFlow,
                                               implicit val statusService: StatusService,
                                               val authConnector: AuthConnector) extends BaseController {

  val NAME = "receivePayments"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(NAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(NAME, "error.required.hvd.receive.cash.payments")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit(HighValueDealing) flatMap {
        case true =>
          cacheConnector.fetch[Hvd](Hvd.key) map {
            response =>
              val form: Form2[Boolean] = (for {
                hvd <- response
                receivePayments <- hvd.receiveCashPayments
              } yield Form2[Boolean](receivePayments)).getOrElse(EmptyForm)
              Ok(receiving(form, edit))
          }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[Boolean](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(receiving(f, edit)))
        case ValidForm(_, data) => {
          for {
            hvd <- cacheConnector.fetch[Hvd](Hvd.key)
            _ <- cacheConnector.save[Hvd](Hvd.key, {
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
      case (_, true, _) => Redirect(routes.SummaryController.get())
      case _ => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get(edit))
    }
}
