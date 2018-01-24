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

package controllers.payments

import javax.inject.Inject

import audit.BacsPaymentEvent
import cats.data.OptionT
import cats.implicits._
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.payments.TypeOfBank
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class TypeOfBankController @Inject()(
                                      val authConnector: AuthConnector,
                                      val auditConnector: AuditConnector,
                                      val statusService: StatusService,
                                      val submissionResponseService: SubmissionResponseService,
                                      val authEnrolmentsService: AuthEnrolmentsService,
                                      val feeResponseService: FeeResponseService,
                                      val paymentsService: PaymentsService
                                    ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(views.html.payments.type_of_bank(EmptyForm)))
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[TypeOfBank](request.body) match {
          case ValidForm(_, data) =>

            doAudit(data.isUK) map { _ =>
              Redirect(controllers.payments.routes.BankDetailsController.get(data.isUK).url)
            }

          case f: InvalidForm => Future.successful(BadRequest(views.html.payments.type_of_bank(f)))
        }
  }

  private def doAudit(ukBank: Boolean)(implicit hc: HeaderCarrier, ac: AuthContext) = {
    (for {
      amlsRegistrationNumber <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
      fees <- OptionT(feeResponseService.getFeeResponse(amlsRegistrationNumber))
      payRef <- OptionT.fromOption[Future](fees.paymentReference)
      amount <- OptionT.fromOption[Future](paymentsService.amountFromSubmissionData(fees))
      result <- OptionT.liftF(auditConnector.sendEvent(BacsPaymentEvent(ukBank, amlsRegistrationNumber, payRef, amount)))
    } yield result).value
  }

}
