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

package controllers.eab

import cats.implicits._
import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}

import javax.inject.Inject
import models.eab.Eab
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ProxyCacheService
import utils.AuthAction
import models.businessmatching.BusinessActivity.{EstateAgentBusinessService => EAB}
import services.businessmatching.ServiceFlow
import utils.DateOfChangeHelper

class EabController @Inject() (
  proxyCacheService: ProxyCacheService,
  authAction: AuthAction,
  val cacheConnector: DataCacheConnector,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  val serviceFlow: ServiceFlow
) extends AmlsBaseController(ds, cc)
    with DateOfChangeHelper {

  def get(credId: String): Action[AnyContent] = Action.async {
    proxyCacheService.getEab(credId).map {
      _.map(Ok(_: JsValue)).getOrElse(NotFound)
    }
  }

  def set(credId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    proxyCacheService.setEab(credId, request.body).map { _ =>
      Ok(Json.obj("_id" -> credId))
    }
  }

  def requireDateOfChange(credId: String, submissionStatus: String) = Action.async(parse.json) { implicit request =>
    def newEabServices: List[String] = Eab(
      request.body.as[JsObject].value("data").as[JsObject]
    ).services.getOrElse(List())

    for {
      currentEab    <- cacheConnector.fetch[Eab](credId, Eab.key)
      isNewActivity <- serviceFlow.isNewActivity(credId, EAB)
    } yield {

      def currentServices: Option[List[String]] =
        currentEab.getOrElse(Eab(Json.obj())).services

      if (!isNewActivity & dateOfChangApplicable(submissionStatus, currentServices, newEabServices)) {
        Ok(Json.obj("requireDateOfChange" -> true))
      } else {
        Ok(Json.obj("requireDateOfChange" -> false))
      }
    }
  }

  def accept: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      eab <- OptionT(cacheConnector.fetch[Eab](request.credId, Eab.key))
      _   <- OptionT.liftF(cacheConnector.save[Eab](request.credId, Eab.key, eab.copy(hasAccepted = true)))
    } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError(
      "Could not update EAB"
    )
  }
}
