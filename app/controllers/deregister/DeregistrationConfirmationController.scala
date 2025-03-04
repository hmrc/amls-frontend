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

package controllers.deregister

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthAction, BusinessName}
import views.html.deregister.DeregistrationConfirmationView

import javax.inject.Inject

class DeregistrationConfirmationController @Inject() (
  authAction: AuthAction,
  ds: CommonPlayDependencies,
  statusService: StatusService,
  enrolmentService: AuthEnrolmentsService,
  cc: MessagesControllerComponents,
  view: DeregistrationConfirmationView
)(implicit
  dataCacheConnector: DataCacheConnector,
  amlsConnector: AmlsConnector
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    val okResult = for {
      amlsRefNumber <- OptionT(enrolmentService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
      status        <- OptionT.liftF(statusService.getReadStatus(amlsRefNumber, request.accountTypeId))
      businessName  <- BusinessName.getName(request.credId, status.safeId, request.accountTypeId)
    } yield Ok(
      view(
        businessName = businessName,
        amlsRefNumber = amlsRefNumber
      )
    )

    okResult getOrElse InternalServerError("Unable to get Deregistration confirmation")
  }

}
