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

package controllers

import config.{AMLSAuditConnector, AMLSAuthConnector}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.SatisfactionSurvey
import play.api.Logger
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import audit.SurveyEvent
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class SatisfactionSurveyController @Inject()(val auditConnector: AuditConnector,
                                             val authConnector: AuthConnector = AMLSAuthConnector) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.satisfaction_survey(EmptyForm)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
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

//object SatisfactionSurveyController extends SatisfactionSurveyController {
//  // $COVERAGE-OFF$
//  override val authConnector = AMLSAuthConnector
//  override val auditConnector = AMLSAuditConnector
//}
