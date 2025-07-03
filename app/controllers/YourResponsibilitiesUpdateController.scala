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

package controllers

import controllers.{AmlsBaseController, CommonPlayDependencies}
import play.api.mvc._
import utils.AuthAction
import views.html.registrationamendment.YourResponsibilitiesUpdateView
import scala.concurrent.Future
import javax.inject.Inject

class YourResponsibilitiesUpdateController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  yourResponsibilitiesUpdateView: YourResponsibilitiesUpdateView
) extends AmlsBaseController(ds, cc) {

  def get(flow: String): Action[AnyContent] = authAction.async { implicit request =>
    Future.successful(Ok(yourResponsibilitiesUpdateView(flow)))
  }
}
