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

package controllers.asp

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms._
import javax.inject.Inject
import models.asp.Asp
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.asp.summary

class SummaryController @Inject()(val dataCache: DataCacheConnector,
                                  val serviceFlow: ServiceFlow,
                                  val statusService: StatusService,
                                  authAction: AuthAction) extends DefaultBaseController {
  def get = authAction.async {
      implicit request =>
        dataCache.fetch[Asp](request.credId, Asp.key) map {
          case Some(data) =>
            Ok(summary(EmptyForm, data))
          case _ =>
            Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post = authAction.async {
      implicit request =>
        for {
          asp <- dataCache.fetch[Asp](request.credId, Asp.key)
          _ <- dataCache.save[Asp](request.credId, Asp.key, asp.copy(hasAccepted = true))
        } yield Redirect(controllers.routes.RegistrationProgressController.get())
  }
}