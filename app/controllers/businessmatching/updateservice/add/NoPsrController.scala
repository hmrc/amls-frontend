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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.flowmanagement.{AddBusinessTypeFlowModel, NoPSRPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.flowmanagement.Router
import utils.AuthAction
import views.html.businessmatching.updateservice.add.CannotAddServicesView

import javax.inject.{Inject, Singleton}

@Singleton
class NoPsrController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  val helper: AddBusinessTypeHelper,
  val router: Router[AddBusinessTypeFlowModel],
  val cc: MessagesControllerComponents,
  view: CannotAddServicesView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction { implicit request =>
    Ok(view())
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      _     <- helper.clearFlowModel(request.credId)
      route <- OptionT.liftF(router.getRoute(request.credId, NoPSRPageId, AddBusinessTypeFlowModel()))
    } yield route) getOrElse InternalServerError("Post: Cannot retrieve data: NoPsrController")
  }
}
