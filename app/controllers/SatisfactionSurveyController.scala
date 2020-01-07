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

package controllers

import audit.SurveyEvent
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.SatisfactionSurvey
import play.api.Logger
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.AuthAction

import scala.concurrent.Future

@Singleton
class SatisfactionSurveyController @Inject()(val auditConnector: AuditConnector,
                                             authAction: AuthAction) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.satisfaction_survey(EmptyForm)))
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[SatisfactionSurvey](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.satisfaction_survey(f)))
        case ValidForm(_, data) => {
          auditConnector.sendEvent(SurveyEvent(data)).onFailure {
            case e: Throwable => Logger.error(s"[SatisfactionSurveyController][post] ${e.getMessage}", e)
          }
          Future.successful(Redirect(routes.LandingController.get()))
        }
      }
    }
  }
}
