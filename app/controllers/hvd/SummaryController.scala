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

package controllers.hvd

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}

import javax.inject.Inject
import models.hvd.Hvd
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import utils.hvd.CheckYourAnswersHelper
import views.html.hvd.CheckYourAnswersView

class SummaryController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCache: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  cyaHelper: CheckYourAnswersHelper,
  view: CheckYourAnswersView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    for {
      hvd <- dataCache.fetch[Hvd](request.credId, Hvd.key)
    } yield hvd match {
      case Some(data) => Ok(view(cyaHelper.getSummaryList(data)))
      case _          => Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      hvd <- OptionT(dataCache.fetch[Hvd](request.credId, Hvd.key))
      _   <- OptionT.liftF(dataCache.save[Hvd](request.credId, Hvd.key, hvd.copy(hasAccepted = true)))
    } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError(
      "Could not update HVD"
    )
  }
}
