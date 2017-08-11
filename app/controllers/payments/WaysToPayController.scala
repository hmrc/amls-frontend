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

package controllers.payments

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.PayApiConnector
import controllers.{BaseController, routes}
import forms.{EmptyForm, Form2, ValidForm}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import models.payments.{CreatePaymentResponse, WaysToPay}
import models.payments.WaysToPay._
import services.{AuthEnrolmentsService, PaymentsService, StatusService, SubmissionResponseService}

import scala.concurrent.Future

@Singleton
class WaysToPayController @Inject()(
                                     val authConnector: AuthConnector,
                                     val paymentsConnector: PayApiConnector,
                                     val statusService: StatusService,
                                     val paymentsService: PaymentsService,
                                     val submissionResponseService: SubmissionResponseService,
                                     val authEnrolmentsService: AuthEnrolmentsService
                                   ) extends BaseController{

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.payments.ways_to_pay(EmptyForm)))
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[WaysToPay](request.body) match {
          case ValidForm(_, data) => data match {
            case Card => {
              (for {
                data@(payRef,_,_,_) <- OptionT(paymentsService.getAmendmentFees)
                amlsRefNo <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
                paymentsRedirect <- OptionT.liftF(paymentsService.requestPaymentsUrl(
                  data,
                  controllers.routes.ConfirmationController.paymentConfirmation(payRef).url,
                  amlsRefNo
                ))
              } yield Redirect(paymentsRedirect.links.nextUrl)) getOrElse Redirect(CreatePaymentResponse.default.links.nextUrl)
            }
            case Bacs => Future.successful(Redirect(controllers.payments.routes.TypeOfBankController.get()))
          }
        }
  }

}
