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
import config.{AMLSAuditConnector, ApplicationConfig, WSHttp}
import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import models.governmentgateway.{EnrolmentRequest, EnrolmentResponse}
import play.api.Logger.{debug, warn}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, HttpResponse}

trait GovernmentGatewayConnector {

  protected[connectors] def http: CorePost
  protected def enrolUrl: String
  private[connectors] def audit: Audit

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
      case e: Throwable if e.getMessage.contains(GovernmentGatewayConnector.duplicateEnrolmentMessage) =>
        warn(msg(s"'${e.getMessage}' error encountered"))
        Future.failed(DuplicateEnrolmentException(e.getMessage, e))
      case e: Throwable if e.getMessage.contains(GovernmentGatewayConnector.invalidCredentialsMessage) =>
        warn(msg(s"'${e.getMessage}' error encountered"))
        Future.failed(InvalidEnrolmentCredentialsException(e.getMessage, e))
      case e =>
        warn(msg("Failure response"))
        Future.failed(e)
    }
  }
}

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  override val http: CorePost = WSHttp
  override val enrolUrl: String = ApplicationConfig.enrolUrl
  override private[connectors] val audit = new Audit("amls-frontend", AMLSAuditConnector)
  private[connectors] val duplicateEnrolmentMessage = "The service HMRC-MLR-ORG requires unique identifiers"
  private[connectors] val invalidCredentialsMessage = "The credential has the wrong type of role"
}
