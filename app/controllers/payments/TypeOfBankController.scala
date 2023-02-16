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

package controllers.payments

import audit.BacsPaymentEvent
import cats.data.OptionT
import cats.implicits._
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.payments.TypeOfBank
import play.api.mvc.MessagesControllerComponents
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, DeclarationHelper}
import views.html.payments.type_of_bank


import scala.concurrent.Future

class TypeOfBankController @Inject()(val authAction: AuthAction,
                                     val ds: CommonPlayDependencies,
                                     val auditConnector: AuditConnector,
                                     val authEnrolmentsService: AuthEnrolmentsService,
                                     val feeResponseService: FeeResponseService,
                                     val paymentsService: PaymentsService,
                                     val cc: MessagesControllerComponents,
                                     val statusService: StatusService,
                                     val renewalService: RenewalService,
                                     type_of_bank: type_of_bank) extends AmlsBaseController(ds, cc) {

  def get() = authAction.async {
    implicit request =>
      (for {
        subHeading <- DeclarationHelper.getSubheadingBasedOnStatus(request.credId, request.amlsRefNumber, request.accountTypeId, statusService, renewalService)
      } yield {
        Ok(type_of_bank(EmptyForm, subHeading))
      }) getOrElse InternalServerError("Failed to retrieve data.")
  }

  def post() = authAction.async {
    implicit request =>
      Form2[TypeOfBank](request.body) match {
        case ValidForm(_, data) =>

          doAudit(data.isUK, request.amlsRefNumber, request.accountTypeId).map { _ =>
            Redirect(controllers.payments.routes.BankDetailsController.get(data.isUK).url)
          }

        case f: InvalidForm => (for {
          subHeading <- DeclarationHelper.getSubheadingBasedOnStatus(request.credId, request.amlsRefNumber, request.accountTypeId, statusService, renewalService)
        } yield {
          BadRequest(type_of_bank(f, subHeading))
        }) getOrElse InternalServerError("Failed to retrieve data.")
      }
  }

  private def doAudit(ukBank: Boolean, amlsRefNumber: Option[String], accountTypeId: (String, String))(implicit hc: HeaderCarrier) = {
    (for {
      ref <- OptionT(Future(amlsRefNumber))
      fees <- OptionT(feeResponseService.getFeeResponse(ref, accountTypeId))
      payRef <- OptionT.fromOption[Future](fees.paymentReference)
      amount <- OptionT.fromOption[Future](paymentsService.amountFromSubmissionData(fees))
      result <- OptionT.liftF(auditConnector.sendEvent(BacsPaymentEvent(ukBank, ref, payRef, amount)))
    } yield result).value
  }

}
