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

import cats.data.OptionT
import cats.implicits._
import controllers.BaseController
import javax.inject.Inject
import models.confirmation.Currency
import models.status.{SubmissionReady, SubmissionReadyForReview}
import services.{AuthEnrolmentsService, FeeResponseService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BankDetailsController @Inject()(
                                      val authConnector: AuthConnector,
                                      val authEnrolmentsService: AuthEnrolmentsService,
                                      val feeResponseService: FeeResponseService,
                                      val statusService: StatusService
                                    ) extends BaseController{


  def get(isUK: Boolean = true) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          status <- OptionT.liftF(statusService.getStatus)
          amlsRegistrationNumber <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
          fees <- OptionT(feeResponseService.getFeeResponse(amlsRegistrationNumber))
          paymentReference <- OptionT.fromOption[Future](fees.paymentReference)
        } yield {
          val amount = fees.differenceOrTotalAmount
          Ok(views.html.payments.bank_details(isUK, amount, paymentReference))
        }) getOrElse InternalServerError("Failed to retrieve submission data")
  }

}