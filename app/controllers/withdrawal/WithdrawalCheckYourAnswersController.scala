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
import services.AuthEnrolmentsService
import utils.{AckRefGenerator, AuthAction, AuthorisedRequest}
import views.html.withdrawal.WithdrawalCheckYourAnswersView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.Future
import cats.implicits._
import cats.instances.future._
import cats.data.EitherT

class WithdrawalCheckYourAnswersController @Inject() (
  authAction: AuthAction,
  ds: CommonPlayDependencies,
  dataCacheConnector: DataCacheConnector,
  amlsConnector: AmlsConnector,
  authEnrolmentsService: AuthEnrolmentsService,
  cc: MessagesControllerComponents,
  view: WithdrawalCheckYourAnswersView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    fetchWithdrawalReason()
      .map { withdrawalReason =>
        Ok(view(withdrawalReason = withdrawalReason))
      }
      .value
      .map {
        case Right(result) => result
        case Left(error)   => error
      }
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    val eitherT: EitherT[Future, Result, Result] = for {
      withdrawalReason           <- fetchWithdrawalReason()
      withdrawSubscriptionRequest = WithdrawSubscriptionRequest(
                                      acknowledgementReference = AckRefGenerator(),
                                      withdrawalDate = LocalDate.now(),
                                      withdrawalReason = withdrawalReason,
                                      withdrawalReasonOthers = withdrawalReasonOthers(withdrawalReason)
                                    )

      amlsRegistrationNumber <-
        EitherT.fromOptionF(
          authEnrolmentsService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier),
          redirectToLandingPage
        )
      _                      <- EitherT.right(
                                  amlsConnector.withdraw(
                                    amlsRegistrationNumber = amlsRegistrationNumber,
                                    request = withdrawSubscriptionRequest,
                                    accountTypeId = request.accountTypeId
                                  )
                                )

    } yield Redirect(controllers.withdrawal.routes.WithdrawalConfirmationController.get)

    eitherT.value.map(_.fold(identity, identity))
  }

  private val redirectToLandingPage = Redirect(controllers.routes.LandingController.start(true))

  private def fetchWithdrawalReason()(implicit
    request: AuthorisedRequest[AnyContent]
  ): EitherT[Future, Result, WithdrawalReason] =
    EitherT.fromOptionF[Future, Result, WithdrawalReason](
      dataCacheConnector.fetch[WithdrawalReason](request.credId, WithdrawalReason.key),
      redirectToLandingPage
    )

  private def withdrawalReasonOthers(withdrawalReason: WithdrawalReason): Option[String] = withdrawalReason match {
    case WithdrawalReason.Other(reason) => Some(reason)
    case _                              => None
  }
}
