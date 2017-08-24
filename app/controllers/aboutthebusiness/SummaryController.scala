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

package controllers.aboutthebusiness

import cats.data.OptionT
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness.AboutTheBusiness
import models.businessmatching.BusinessMatching
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import views.html.aboutthebusiness._

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector
  val statusService: StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
        status <- statusService.getStatus
        businessMatching <- dataCache.fetch[BusinessMatching](BusinessMatching.key)
      } yield aboutTheBusiness match {
        case Some(data) => {
          val showRegisteredForMLR = status match {
            case NotCompleted | SubmissionReady | SubmissionReadyForReview => true
            case _ => false
          }

          val maybeBT = for {
            bm <- businessMatching
            rd <- bm.reviewDetails
            bt <- rd.businessType
          } yield {
            Ok(summary(data, showRegisteredForMLR, bt))
          }

          maybeBT.getOrElse(Redirect(controllers.routes.RegistrationProgressController.get()))

        }
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
