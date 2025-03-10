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

package controllers.payments

import audit.BacsPaymentEvent
import cats.data.OptionT
import cats.implicits._
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.payments.TypeOfBankFormProvider
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, DeclarationHelper}
import views.html.payments.TypeOfBankView

import javax.inject.Inject
import scala.concurrent.Future

class TypeOfBankController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val auditConnector: AuditConnector,
  val authEnrolmentsService: AuthEnrolmentsService,
  val feeResponseService: FeeResponseService,
  val paymentsService: PaymentsService,
  val cc: MessagesControllerComponents,
  val statusService: StatusService,
  val renewalService: RenewalService,
  formProvider: TypeOfBankFormProvider,
  view: TypeOfBankView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      subHeading <- DeclarationHelper.getSubheadingBasedOnStatus(
                      request.credId,
                      request.amlsRefNumber,
                      request.accountTypeId,
                      statusService,
                      renewalService
                    )
    } yield Ok(view(formProvider(), subHeading))) getOrElse InternalServerError("Failed to retrieve data.")
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          (for {
            subHeading <- DeclarationHelper.getSubheadingBasedOnStatus(
                            request.credId,
                            request.amlsRefNumber,
                            request.accountTypeId,
                            statusService,
                            renewalService
                          )
          } yield BadRequest(view(formWithErrors, subHeading))) getOrElse InternalServerError(
            "Failed to retrieve data."
          ),
        data =>
          doAudit(data.isUK, request.amlsRefNumber, request.accountTypeId).map { _ =>
            Redirect(controllers.payments.routes.BankDetailsController.get(data.isUK).url)
          }
      )
  }

  private def doAudit(ukBank: Boolean, amlsRefNumber: Option[String], accountTypeId: (String, String))(implicit
    hc: HeaderCarrier
  ): Future[Option[AuditResult]] =
    (for {
      ref    <- OptionT(Future(amlsRefNumber))
      fees   <- OptionT(feeResponseService.getFeeResponse(ref, accountTypeId))
      payRef <- OptionT.fromOption[Future](fees.paymentReference)
      amount <- OptionT.fromOption[Future](paymentsService.amountFromSubmissionData(fees))
      result <- OptionT.liftF(auditConnector.sendEvent(BacsPaymentEvent(ukBank, ref, payRef, amount)))
    } yield result).value

}
