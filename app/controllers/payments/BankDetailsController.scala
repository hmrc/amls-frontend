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
import cats.data.OptionT
import cats.implicits._
import controllers.BaseController
import models.confirmation.{Currency, SubmissionData}
import models.status.SubmissionReady
import services.{StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BankDetailsController @Inject()(
                                      val authConnector: AuthConnector,
                                      val submissionResponseService: SubmissionResponseService,
                                      val statusService: StatusService
                                    ) extends BaseController{


  def get(isUK: Boolean = true) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          status <- OptionT.liftF(statusService.getStatus)
          SubmissionData(payRef, fees, _, _, difference) <- OptionT(submissionResponseService.getSubmissionData(status))
          paymentReference <- OptionT.fromOption[Future](payRef)
        } yield {
          val amount = if (status == SubmissionReady) fees else difference.getOrElse(Currency(0))

          Ok(views.html.payments.bank_details(isUK, amount, paymentReference))
        }) getOrElse InternalServerError("Failed to retrieve submission data")
  }

}