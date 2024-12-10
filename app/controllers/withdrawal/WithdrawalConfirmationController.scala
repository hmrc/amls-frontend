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

package controllers.withdrawal

import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawalReason}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuthEnrolmentsService, StatusService}
import utils.{AckRefGenerator, AuthAction, AuthorisedRequest, BusinessName}
import views.html.withdrawal.{WithdrawalCheckYourAnswersView, WithdrawalConfirmationView}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.Future
import cats.implicits._
import cats.instances.future._
import cats.data.{EitherT, OptionT}
import views.html.deregister.DeregistrationConfirmationView

class WithdrawalConfirmationController @Inject()(
                                                  authAction: AuthAction,
                                                  ds: CommonPlayDependencies,
                                                  statusService: StatusService,
                                                  enrolmentService: AuthEnrolmentsService,
                                                  cc: MessagesControllerComponents,
                                                  view: WithdrawalConfirmationView)(implicit
                                                  dataCacheConnector: DataCacheConnector,
                                                  amlsConnector: AmlsConnector
                                                ) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>

    val okResult = for {
      amlsRefNumber <- OptionT(enrolmentService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
      status <- OptionT.liftF(statusService.getReadStatus(amlsRefNumber, request.accountTypeId))
      reason <- OptionT(dataCacheConnector.fetch[WithdrawalReason](request.credId, WithdrawalReason.key))
      businessName <- BusinessName.getName(request.credId, status.safeId, request.accountTypeId)
    } yield Ok(view(
      businessName = businessName,
      amlsRefNumber = amlsRefNumber,
      withdrawalReason = reason
    ))

    okResult getOrElse InternalServerError("Unable to get Withdrawal confirmation")
  }


}
