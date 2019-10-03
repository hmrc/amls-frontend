/*
 * Copyright 2019 HM Revenue & Customs
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
import config.{AppConfig, CachedStaticHtmlPartialProvider}
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc.{MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.ControllerHelper

import scala.concurrent.ExecutionContext

abstract class AmlsBaseController(val cpd: CommonPlayDependencies, override val controllerComponents: MessagesControllerComponents) extends FrontendController(controllerComponents) with I18nSupport {

  override implicit val messagesApi: MessagesApi = cpd.messagesApi
  implicit val partialProvider: CachedStaticHtmlPartialProvider = cpd.partialProvider

  implicit val lang: Lang = Lang.defaultLang

  implicit val ec: ExecutionContext = controllerComponents.executionContext

  val messages: MessagesApi = messagesApi

  def notFoundView(implicit request: Request[_]) = ControllerHelper.notFoundView(request, partialProvider)
}

class CommonPlayDependencies @Inject()(val amlsConfig: AppConfig,
                                       val messagesApi: MessagesApi,
                                       val partialProvider: CachedStaticHtmlPartialProvider)
