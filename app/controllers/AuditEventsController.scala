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

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class AuditEventsController @Inject()(val authConnector: AuthConnector, auditConnector: AuditConnector)
  extends BaseController {

  def sendAuditEvent(): Action[AnyContent] = Authorised.async {
    implicit authContext =>
      implicit request =>
        doSendEvent
  }

  private def doSendEvent(implicit hc: HeaderCarrier) =  {
    auditConnector.sendEvent(DataEvent("Amls-frontend", "timeout event"))
    Future(Ok)
  }
}
