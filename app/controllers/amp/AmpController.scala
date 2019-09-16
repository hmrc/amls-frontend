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

package controllers.amp

import controllers.DefaultBaseController
import javax.inject.Inject
import services.amp.AmpService
import utils.AuthAction
import play.api.libs.json._

class AmpController @Inject()(ampService: AmpService,
                              authAction: AuthAction) extends DefaultBaseController {

  def get(credId: String) = authAction.async(parse.json) {
    implicit request => {
      ampService.get(credId).map {
        _.map(Ok(_: JsValue)).getOrElse(NotFound)
      }
    }
  }

  def set(credId: String) = authAction.async(parse.json) {
    implicit request => {
      ampService.set(credId, request.body).map {
        r => {
          Ok(Json.toJson(r))
        }
      }
    }
  }
}
