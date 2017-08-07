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

package controllers.withdrawal

import javax.inject.Inject

import cats.implicits._
import cats.data.OptionT
import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawalReason}
import org.joda.time.LocalDate
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.FeatureToggle
import views.html.withdrawal.withdrawal_reason
import utils.AckRefGenerator

import scala.concurrent.Future

class WithdrawalReasonController @Inject()(
                                            val authConnector: AuthConnector,
                                            val amls: AmlsConnector,
                                            enrolments: AuthEnrolmentsService,
                                            statusService: StatusService) extends BaseController {

  def get = FeatureToggle(ApplicationConfig.allowWithdrawalToggle) {
    Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(withdrawal_reason(EmptyForm)))
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      Form2[WithdrawalReason](request.body) match {
        case f:InvalidForm => Future.successful(BadRequest(withdrawal_reason(f)))
        case ValidForm(_, data) => {
          val withdrawalReasonOthers = data match {
            case WithdrawalReason.Other(reason) => reason.some
            case _ => None
          }
          val withdrawal = WithdrawSubscriptionRequest(
            AckRefGenerator(),
            LocalDate.now(),
            data,
            withdrawalReasonOthers
          )
          (for {
            regNumber <- OptionT(enrolments.amlsRegistrationNumber)
            _ <- OptionT.liftF(amls.withdraw(regNumber, withdrawal))
          } yield Redirect(controllers.routes.LandingController.get())) getOrElse InternalServerError("Unable to withdraw the application")
        }
      }
  }

}
