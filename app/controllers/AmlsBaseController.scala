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

import com.google.inject.Inject
import config.ApplicationConfig
import play.api.i18n.{Lang, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.mvc.{AnyContent, BodyParsers, MessagesActionBuilderImpl, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ControllerHelper

import scala.concurrent.ExecutionContext

abstract class AmlsBaseController(
  val cpd: CommonPlayDependencies,
  override val controllerComponents: MessagesControllerComponents
) extends FrontendController(controllerComponents) {

  override implicit val messagesApi: MessagesApi = cpd.messagesApi

  implicit val ec: ExecutionContext = controllerComponents.executionContext

  implicit val appConfig: ApplicationConfig = cpd.amlsConfig

  implicit val lang: Lang = Lang.defaultLang

  val messages: MessagesApi = messagesApi

  implicit val messagesProvider: MessagesProvider =
    MessagesImpl(lang, messagesApi)

  def notFoundView(implicit request: Request[_], messages: play.api.i18n.Messages, error: views.html.ErrorView) =
    ControllerHelper.notFoundView
}

class CommonPlayDependencies @Inject() (val amlsConfig: ApplicationConfig, val messagesApi: MessagesApi)

trait MessagesRequestHelper {
  def messagesAction(
    parsers: BodyParsers.Default
  )(implicit executionContext: ExecutionContext, messagesApi: MessagesApi) =
    new MessagesActionBuilderImpl[AnyContent](parsers, messagesApi)
}
