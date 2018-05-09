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

package controllers.businessactivities

import controllers.BaseController
import javax.inject.{Inject, Singleton}
import models.status._
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

@Singleton
class WhatYouNeedController @Inject()(val authConnector: AuthConnector, statusService: StatusService) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>

      statusService.getStatus map { status =>
        val nextPageUrl = status match {
          case NotCompleted | SubmissionReady | SubmissionReadyForReview =>
            routes.InvolvedInOtherController.get().url
          case _ =>
            routes.BusinessFranchiseController.get().url
        }

        Ok(what_you_need(nextPageUrl))
      }
  }
}