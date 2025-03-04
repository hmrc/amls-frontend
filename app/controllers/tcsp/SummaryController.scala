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

package controllers.tcsp

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.tcsp._
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import utils.tcsp.CheckYourAnswersHelper
import views.html.tcsp.CheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.Future

class SummaryController @Inject() (
  val dataCache: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val serviceFlow: ServiceFlow,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  cyaHelper: CheckYourAnswersHelper,
  val view: CheckYourAnswersView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with Logging {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    fetchModel(request.credId) map {
      case Some(data) if data.copy(hasAccepted = true).isComplete =>
        Ok(view(cyaHelper.getSummaryList(data)))
      case _                                                      => Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      model <- OptionT(fetchModel(request.credId))
      _     <- OptionT.liftF(dataCache.save[Tcsp](request.credId, Tcsp.key, model.copy(hasAccepted = true)))
    } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError(
      "Cannot update Tcsp"
    )
  }

  private def fetchModel(credId: String): Future[Option[Tcsp]] = dataCache.fetch[Tcsp](credId, Tcsp.key)
}
