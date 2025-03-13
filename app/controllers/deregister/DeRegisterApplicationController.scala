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
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName}
import views.html.deregister.DeregisterApplicationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeRegisterApplicationController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cache: DataCacheConnector,
  val statusService: StatusService,
  enrolments: AuthEnrolmentsService,
  val amls: AmlsConnector,
  val cc: MessagesControllerComponents,
  view: DeregisterApplicationView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      amlsRegNumber <- OptionT(enrolments.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
      id            <- OptionT(statusService.getSafeIdFromReadStatus(amlsRegNumber, request.accountTypeId, request.credId))
      name          <- BusinessName.getName(request.credId, Some(id), request.accountTypeId)(
                         implicitly[HeaderCarrier],
                         implicitly[ExecutionContext],
                         cache,
                         amls
                       )
    } yield Ok(view(name))) getOrElse InternalServerError("Could not show the de-register page")
  }

  def post(): Action[AnyContent] = authAction {
    Redirect(routes.DeregistrationReasonController.get)
  }
}
