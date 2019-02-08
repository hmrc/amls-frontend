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

package controllers.aboutthebusiness

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.AboutTheBusiness
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.aboutthebusiness._


class SummaryController @Inject () (
                                   val dataCache: DataCacheConnector,
                                   val statusService: StatusService,
                                   val authConnector: AuthConnector
                                   ) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
        status <- statusService.getStatus
      } yield aboutTheBusiness match {
        case Some(data) => {
          val showRegisteredForMLR = status match {
            case NotCompleted | SubmissionReady | SubmissionReadyForReview => true
            case _ => false
          }
          Ok(summary(EmptyForm, data, showRegisteredForMLR))
        }
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
        _ <- dataCache.save[AboutTheBusiness](AboutTheBusiness.key,
          aboutTheBusiness.copy(hasAccepted = true)
        )
      } yield {
        Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}
