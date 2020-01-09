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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms._
import javax.inject.{Inject, Singleton}
import utils.{AuthAction, RepeatingSection}

import scala.concurrent.Future

@Singleton
class FitAndProperNoticeController @Inject()(val dataCacheConnector: DataCacheConnector,
                                             authAction: AuthAction
                                            ) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
        Future(Ok(views.html.responsiblepeople.fit_and_proper_notice(EmptyForm, edit, index, flow, "")))
  }
}
