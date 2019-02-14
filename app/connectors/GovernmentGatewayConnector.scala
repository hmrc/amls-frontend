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

package connectors

import audit.EnrolEvent
import config.{AMLSAuditConnector, AppConfig, WSHttp}
import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import javax.inject.Inject
import models.governmentgateway.EnrolmentRequest
import play.api.Logger.{debug, warn}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import utils.AuditHelper

import scala.concurrent.{ExecutionContext, Future}

class GovernmentGatewayConnector @Inject()(protected[connectors] val http: WSHttp,
                                           private[this] val appConfig: AppConfig,
                                           private[connectors] val audit: Audit = new Audit(AuditHelper.appName, AMLSAuditConnector)
                                          ) {

  protected def enrolUrl = appConfig.enrolUrl
  private[connectors] val duplicateEnrolmentMessage = "The service HMRC-MLR-ORG requires unique identifiers"
  private[connectors] val invalidCredentialsMessage = "The credential has the wrong type of role"

  private def msg(msg: String) = s"[GovernmentGatewayConnector][enrol] - $msg"

  def enrol
  (request: EnrolmentRequest)
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   reqW: Writes[EnrolmentRequest]
  ): Future[HttpResponse] = {

    debug(msg(s"Request body: ${Json.toJson(request)}"))

    http.POST[EnrolmentRequest, HttpResponse](enrolUrl, request) map {
      response =>
        audit.sendDataEvent(EnrolEvent(request, response))
        debug(msg(s"Successful Response: ${response.json}"))
        response
    } recoverWith {
      case e: Throwable if e.getMessage.contains(duplicateEnrolmentMessage) =>
        warn(msg(s"'${e.getMessage}' error encountered"))
        Future.failed(DuplicateEnrolmentException(e.getMessage, e))
      case e: Throwable if e.getMessage.contains(invalidCredentialsMessage) =>
        warn(msg(s"'${e.getMessage}' error encountered"))
        Future.failed(InvalidEnrolmentCredentialsException(e.getMessage, e))
      case e =>
        warn(msg("Failure response"))
        Future.failed(e)
    }
  }
}
