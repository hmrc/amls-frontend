/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.supervision

import controllers.{AmlsBaseController, CommonPlayDependencies}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.supervision.WhatYouNeedView

import javax.inject.Inject

class WhatYouNeedController @Inject() (val authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       val cc: MessagesControllerComponents,
                                       view: WhatYouNeedView) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction { implicit request =>
      Ok(view(controllers.supervision.routes.AnotherBodyController.get()))
    }
}
