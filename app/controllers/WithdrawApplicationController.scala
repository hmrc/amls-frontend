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

package controllers

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawalReason}
import org.joda.time.LocalDate
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.withdraw_application

import scala.concurrent.Future

class WithdrawApplicationController @Inject()
(val authConnector: AuthConnector,
 amls: AmlsConnector,
 enrolments: AuthEnrolmentsService,
 cache: DataCacheConnector,
 statusService: StatusService) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>

      val maybeProcessingDate = for {
        status <- OptionT.liftF(statusService.getDetailedStatus)
        response <- OptionT.fromOption[Future](status._2)
      } yield response.processingDate

      (for {
        cache <- OptionT(cache.fetch[BusinessMatching](BusinessMatching.key))
        details <- OptionT.fromOption[Future](cache.reviewDetails)
        processingDate <- maybeProcessingDate
      } yield Ok(withdraw_application(details.businessName, processingDate))) getOrElse InternalServerError("Unable to show the withdrawal page")
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      val requestData = WithdrawSubscriptionRequest(WithdrawSubscriptionRequest.DefaultAckReference, LocalDate.now(), WithdrawalReason.OutOfScope)

      (for {
        regNumber <- OptionT(enrolments.amlsRegistrationNumber)
        _ <- OptionT.liftF(amls.withdraw(regNumber, requestData))
      } yield Redirect(routes.LandingController.get())) getOrElse InternalServerError("Unable to withdraw the application")
  }

}
