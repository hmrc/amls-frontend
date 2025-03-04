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

package controllers

import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc._
import utils.AuthAction
import views.html.{UnauthorisedRoleView, UnauthorisedView}
import scala.concurrent.Future

class AmlsController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  implicit override val messagesApi: MessagesApi,
  parser: BodyParsers.Default,
  unauthorisedView: UnauthorisedView,
  unauthorisedRole: UnauthorisedRoleView
) extends AmlsBaseController(ds, cc)
    with MessagesRequestHelper {

  val unauthorised: Action[AnyContent] = messagesAction(parser).async { implicit request: MessagesRequest[AnyContent] =>
    Future.successful(Ok(unauthorisedView()))
  }

  val unauthorised_role = messagesAction(parser).async { implicit request: MessagesRequest[AnyContent] =>
    Future.successful(Unauthorized(unauthorisedRole()))
  }

  val keep_alive = authAction.async {
    Future.successful(Ok("OK"))
  }
}
