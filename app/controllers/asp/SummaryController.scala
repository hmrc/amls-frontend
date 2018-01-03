/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.asp.Asp
import views.html.asp.summary
import forms._
import play.api.Play
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import models.businessmatching.AccountancyServices
import cats.implicits._
import cats.data.OptionT

class SummaryController @Inject()(dataCache: DataCacheConnector, serviceFlow: ServiceFlow, statusService: StatusService, val authConnector: AuthConnector) extends BaseController {
  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCache.fetch[Asp](Asp.key) map {
          case Some(data) =>
            Ok(summary(EmptyForm, data))
          case _ =>
            Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          asp <- dataCache.fetch[Asp](Asp.key)
          _ <- dataCache.save[Asp](Asp.key, asp.copy(hasAccepted = true))
          preSubmission <- statusService.isPreSubmission
          inNewServiceFlow <- serviceFlow.inNewServiceFlow(AccountancyServices)
        } yield (preSubmission, inNewServiceFlow) match {
          case (false, true) => Redirect(controllers.businessmatching.updateservice.routes.NewServiceInformationController.get())
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }
}