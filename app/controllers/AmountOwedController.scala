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

package controllers

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import javax.inject.Inject
import models.SubmissionRequestStatus
import services.{AuthEnrolmentsService, FeeResponseService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class AmountOwedController @Inject()(val dataCacheConnector: DataCacheConnector,
                                     val authConnector: AuthConnector,
                                     val authEnrolmentsService: AuthEnrolmentsService,
                                     val feeResponseService: FeeResponseService,
                                     val statusService: StatusService
                                    ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
      (for {
        submissionRequestStatus <- OptionT.liftF(dataCacheConnector.fetch[SubmissionRequestStatus](SubmissionRequestStatus.key))
        status <- OptionT.liftF(statusService.getStatus)
        amlsRegistrationNumber <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
        fees <- OptionT(feeResponseService.getFeeResponse(amlsRegistrationNumber))
      } yield {
        val amount = fees.toPay(status, submissionRequestStatus)
        Ok(views.html.amount_owed(amount, controllers.payments.routes.WaysToPayController.get().url))
      }) getOrElse InternalServerError("Failed to display page")
  }
}