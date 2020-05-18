/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.accessibility

import java.net.URLEncoder

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.Referer
import config.ApplicationConfig
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Inject
import play.api.mvc.{Call, MessagesControllerComponents}
import utils.AuthAction
import views.html.accessibility._

import scala.concurrent.Future

class AccessibilityStatementController @Inject()(authAction: AuthAction,
                                                 val ds: CommonPlayDependencies,
                                                 val cc: MessagesControllerComponents,
                                                 config: ApplicationConfig) extends AmlsBaseController(ds, cc) {

  def get = authAction.async {
      implicit request =>

        val baseUrl = config.accessibilityStatementUrlUnauthenticated

        val pageUrl = Uri(request.headers.get("referer").getOrElse(
          controllers.accessibility.routes.AccessibilityStatementController.get().url
        ))

        val accessibilityUrl = s"${baseUrl}?service=amls&userAction=${
          URLEncoder.encode(pageUrl.path.toString(), "UTF-8")
        }"

        Future.successful(Ok(accessibility_statement(accessibilityUrl)))
  }
}
