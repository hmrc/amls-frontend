/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.renewal

import controllers.{AmlsBaseController, CommonPlayDependencies, MessagesRequestHelper}
import play.api.i18n.MessagesApi
import play.api.mvc._
import utils.AuthAction
import views.html.renewal.YourResponsibilitiesView

import javax.inject.Inject
import scala.concurrent.Future

class YourResponsibilitiesController @Inject()(
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  implicit override val messagesApi: MessagesApi,
  parser: BodyParsers.Default,
  view: YourResponsibilitiesView,
) extends AmlsBaseController(ds, cc)
    with MessagesRequestHelper {

  val get: Action[AnyContent] = messagesAction(parser).async { implicit request: MessagesRequest[AnyContent] =>
    Future.successful(Ok(view()))
  }

  val get2 = authAction.async {
    Future.successful(Ok("OK"))
  }
}
