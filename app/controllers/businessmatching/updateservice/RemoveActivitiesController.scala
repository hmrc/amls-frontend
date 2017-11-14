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

package controllers.businessmatching.updateservice

import javax.inject.{Inject, Singleton}

import controllers.BaseController
import forms.EmptyForm
import models.businessmatching.BusinessActivities
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class RemoveActivitiesController @Inject()(
                                          val authConnector: AuthConnector,
                                          val businessMatchingService: BusinessMatchingService,
                                          val statusService: StatusService
                                          ) extends BaseController {

  def get = Authorised.async{
    implicit authContext =>
      implicit request =>
        statusService.isPreSubmission flatMap {
          case false => businessMatchingService.getModel.value map { bm =>
            (for {
              businessMatching <- bm
              businessActivities <- businessMatching.activities
            } yield {
              val activities = businessActivities.businessActivities map BusinessActivities.getValue
              Ok(views.html.businessmatching.updateservice.remove_activities(EmptyForm, activities))
            }) getOrElse InternalServerError("Could not retrieve activities")
          }
          case true => Future.successful(NotFound(notFoundView))
        }
  }

  def post = Authorised.async{
    implicit authContext =>
      implicit request =>
        ???
  }

}
