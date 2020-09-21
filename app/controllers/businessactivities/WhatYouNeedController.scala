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

package controllers.businessactivities

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.EmptyForm
import javax.inject.Inject
import models.businessmatching.{BusinessActivities, BusinessMatching, HighValueDealing}
import models.responsiblepeople.ResponsiblePerson
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import uk.gov.hmrc.auth.core.AuthConnector
import utils.{AuthAction, ControllerHelper}

import views.html.businessactivities._
import views.html.deregister.deregistration_reason
import views.html.registrationprogress.registration_progress

class WhatYouNeedController @Inject()(val dataCacheConnector: DataCacheConnector,
                                      val authConnector: AuthConnector,
                                      statusService: StatusService,
                                      authAction: AuthAction,
                                      val ds: CommonPlayDependencies,
                                      val cc: MessagesControllerComponents,
                                      what_you_need: what_you_need) extends AmlsBaseController(ds, cc) {
import scala.concurrent.Future

  def get = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
        (for {
          bm <- businessMatching
          ba <- bm.activities
        } yield {
          Ok(what_you_need(routes.InvolvedInOtherController.get().url, Some(ba)))
        })getOrElse(InternalServerError("Unable to retrieve business activities"))
      }
  }
}