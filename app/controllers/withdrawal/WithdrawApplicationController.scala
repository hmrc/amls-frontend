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

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthAction, BusinessName}
import views.html.withdrawal.WithdrawApplicationView

import javax.inject.Inject
import scala.concurrent.Future

class WithdrawApplicationController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val amls: AmlsConnector,
  implicit val dc: DataCacheConnector,
  enrolments: AuthEnrolmentsService,
  implicit val statusService: StatusService,
  val cc: MessagesControllerComponents,
  view: WithdrawApplicationView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    val maybeProcessingDate = for {
      status   <-
        OptionT.liftF(statusService.getDetailedStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
      response <- OptionT.fromOption[Future](status._2)
    } yield response.processingDate

    (for {
      processingDate <- maybeProcessingDate
      amlsRegNumber  <- OptionT(enrolments.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
      id             <- OptionT(statusService.getSafeIdFromReadStatus(amlsRegNumber, request.accountTypeId, request.credId))
      name           <- BusinessName.getName(request.credId, Some(id), request.accountTypeId)
    } yield Ok(view(name, processingDate))) getOrElse InternalServerError("Unable to show the withdrawal page")
  }

  def post: Action[AnyContent] = authAction {
    Redirect(routes.WithdrawalReasonController.get())
  }

}
