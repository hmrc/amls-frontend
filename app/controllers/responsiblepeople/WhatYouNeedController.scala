/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import controllers.BaseController
import views.html.responsiblepeople._

import scala.concurrent.Future

trait WhatYouNeedController extends BaseController {

  def get(index: Int, fromDeclaration: Option[String]) =
    Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(what_you_need(index, fromDeclaration)))
    }
}

object WhatYouNeedController extends WhatYouNeedController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
}
