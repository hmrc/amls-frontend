/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawalReason}
import org.joda.time.LocalDate
import services.{AuthEnrolmentsService, StatusService}
import utils.{AckRefGenerator, AuthAction}
import views.html.withdrawal.withdrawal_reason

import scala.concurrent.Future

class WithdrawalReasonController @Inject()(
                                            authAction: AuthAction, val ds: CommonPlayDependencies,
                                            val amls: AmlsConnector,
                                            enrolments: AuthEnrolmentsService,
                                            statusService: StatusService,
                                            cacheConnector: DataCacheConnector) extends AmlsBaseController(ds) {

  def get = authAction.async {
    implicit request =>
      Future.successful(Ok(withdrawal_reason(EmptyForm)))
  }

  def post = authAction.async {
      implicit request =>
        Form2[WithdrawalReason](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(withdrawal_reason(f)))
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
              regNumber <- OptionT(enrolments.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
              _ <- OptionT.liftF(amls.withdraw(regNumber, withdrawal, request.accountTypeId))
            } yield Redirect(controllers.routes.LandingController.get())) getOrElse InternalServerError("Unable to withdraw the application")
          }
        }
  }

}
