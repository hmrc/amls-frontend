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

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.PayApiConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.payments.{UpdateBacsRequest, WaysToPay}
import models.payments.WaysToPay._
import services.{AuthEnrolmentsService, PaymentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import models.payments.CreateBacsPaymentRequest
import models.confirmation.{Currency, SubmissionData}
import models.status.NotCompleted
import utils.AmlsRefNumberBroker

import scala.concurrent.Future

@Singleton
class WaysToPayController @Inject()(
                                     val authConnector: AuthConnector,
                                     val paymentsConnector: PayApiConnector,
                                     val statusService: StatusService,
                                     val paymentsService: PaymentsService,
                                     val submissionResponseService: SubmissionResponseService,
                                     val authEnrolmentsService: AuthEnrolmentsService,
                                     val amlsRefBroker: AmlsRefNumberBroker
                                   ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(views.html.payments.ways_to_pay(EmptyForm)))
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
        val submissionDetails = for {
          amlsRefNo <- amlsRefBroker.get
          (status, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus(amlsRefNo))
          data@(SubmissionData(paymentReference, _, _, _, _)) <- OptionT(submissionResponseService.getSubmissionData(status))
          payRef <- OptionT.fromOption[Future](paymentReference)
        } yield (
          amlsRefNo,
          payRef,
          data,
          detailedStatus.fold[String](throw new Exception("No safeID available"))(_.safeId.getOrElse(throw new Exception("No safeID available")))
        )

        Form2[WaysToPay](request.body) match {
          case ValidForm(_, data) => data match {
            case Card => {
              (for {
                (amlsRefNo, payRef, submissionData, safeId) <- submissionDetails
                paymentsRedirect <- OptionT.liftF(paymentsService.requestPaymentsUrl(
                  submissionData,
                  controllers.routes.ConfirmationController.paymentConfirmation(payRef).url,
                  amlsRefNo,
                  safeId
                ))
              } yield Redirect(paymentsRedirect.links.nextUrl)) getOrElse InternalServerError("Cannot retrieve payment information")
            }
            case Bacs =>
              val bankTypeResult = for {
                (amlsRef, payRef, submissionData, safeId) <- submissionDetails
                _ <- OptionT.liftF(paymentsService.createBacsPayment(
                  CreateBacsPaymentRequest(amlsRef, payRef, safeId,
                    paymentsService.amountFromSubmissionData(submissionData).fold(0)(_.map(_ * 100).value.toInt))))
              } yield Redirect(controllers.payments.routes.TypeOfBankController.get())

              bankTypeResult getOrElse InternalServerError("Unable to save BACS info")
          }
          case f: InvalidForm => Future.successful(BadRequest(views.html.payments.ways_to_pay(f)))
        }
  }
}
