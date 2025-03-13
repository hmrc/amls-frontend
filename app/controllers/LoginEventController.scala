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

import config.ApplicationConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import utils.AuthAction

import views.html.LoginEventView

import scala.concurrent.Future

@Singleton
class LoginEventController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  applicationConfig: ApplicationConfig,
  view: LoginEventView
) extends AmlsBaseController(ds, cc) {

  val redirectCallType = "GET"

  def get: Action[AnyContent] = authAction.async { implicit request =>
    Future(Ok(view(generateRedirect(applicationConfig.eabRedressUrl))))
  }

  private def generateRedirect(destinationUrl: String) =
    Call(redirectCallType, destinationUrl)
}
