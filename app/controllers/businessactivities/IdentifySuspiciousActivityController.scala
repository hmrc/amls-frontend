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

package controllers.businessactivities

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import utils.AuthAction
import views.html.businessactivities._

import scala.concurrent.Future

class IdentifySuspiciousActivityController @Inject() ( val dataCacheConnector: DataCacheConnector,
                                                       val authAction: AuthAction
                                                     ) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.cacheId, BusinessActivities.key) map {
        response =>
          val form: Form2[IdentifySuspiciousActivity] = (for {
            businessActivities <- response
            identifySuspiciousActivity <- businessActivities.identifySuspiciousActivity
          } yield Form2[IdentifySuspiciousActivity](identifySuspiciousActivity)).getOrElse(EmptyForm)
          Ok(identify_suspicious_activity(form, edit))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[IdentifySuspiciousActivity](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(identify_suspicious_activity(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.cacheId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.cacheId, BusinessActivities.key,
              businessActivities.identifySuspiciousActivity(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.NCARegisteredController.get())
          }
      }
  }
}
