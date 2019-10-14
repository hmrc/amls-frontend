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

package controllers.renewal

import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.{Inject, Singleton}
import models.registrationprogress.{NotStarted, Section, Started}
import play.api.mvc.MessagesControllerComponents
import services.RenewalService
import utils.AuthAction
import views.html.renewal._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class WhatYouNeedController @Inject()(val authAction: AuthAction,
                                      val ds: CommonPlayDependencies,
                                      renewalService: RenewalService,
                                      val cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends AmlsBaseController(ds, cc) {

  def get = authAction.async {
    implicit request =>
      renewalService.getSection(request.credId) map {
        case Section(_,NotStarted | Started,_,_) => Ok(what_you_need())
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}
