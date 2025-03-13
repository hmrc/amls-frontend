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

package controllers.amp

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}

import javax.inject.Inject
import models.amp.Amp
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ProxyCacheService
import utils.AuthAction

class AmpController @Inject() (
  proxyCacheService: ProxyCacheService,
  authAction: AuthAction,
  val cacheConnector: DataCacheConnector,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents
) extends AmlsBaseController(ds, cc) {

  def get(credId: String): Action[AnyContent] = Action.async {
    proxyCacheService.getAmp(credId).map {
      _.map(Ok(_: JsValue)).getOrElse(NotFound)
    }
  }

  def set(credId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    proxyCacheService.setAmp(credId, request.body).map { _ =>
      Ok(Json.obj("_id" -> credId))
    }
  }

  def accept: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      amp <- OptionT(cacheConnector.fetch[Amp](request.credId, Amp.key))
      _   <- OptionT.liftF(cacheConnector.save[Amp](request.credId, Amp.key, amp.copy(hasAccepted = true)))
    } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError(
      "Could not update AMP"
    )
  }
}
