/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.hvd.{Hvd, PaymentMethods, ReceiveCashPayments}
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.hvd.expect_to_receive

import scala.concurrent.Future

@Singleton
class ExpectToReceiveCashPaymentsController @Inject()(
                                                       val authConnector: AuthConnector = AMLSAuthConnector,
                                                       val cacheConnector: DataCacheConnector,
                                                       implicit val statusService: StatusService,
                                                       implicit val serviceFlow: ServiceFlow
                                                     ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true =>
          cacheConnector.fetch[Hvd](Hvd.key) map {
            response =>
              val form: Form2[PaymentMethods] = (for {
                hvd <- response
                receivePayments <- hvd.cashPaymentMethods
              } yield {
                Form2[PaymentMethods](receivePayments)
              }).getOrElse(EmptyForm)

              Ok(expect_to_receive(form, edit))
          }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[PaymentMethods](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(expect_to_receive(f, edit)))
        case ValidForm(_, data) =>
          for {
            hvd <- cacheConnector.fetch[Hvd](Hvd.key)
            _ <- cacheConnector.save[Hvd](Hvd.key,
              hvd.cashPaymentMethods(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
          }
      }
  }
}