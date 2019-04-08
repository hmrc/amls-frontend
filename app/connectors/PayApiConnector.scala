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

import javax.inject.Inject
import audit.{CreatePaymentEvent, CreatePaymentFailureEvent}
import cats.implicits._
import config.{WSHttp}
import models.payments.{CreatePaymentRequest, CreatePaymentResponse}
import play.api.Mode.Mode
import play.api.{Configuration, Logger, Play}
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class PayApiConnector @Inject()(
                                 http: WSHttp,
                                 auditConnector: AuditConnector
                               ) extends HttpResponseHelper with ServicesConfig {

  lazy val payBaseUrl = s"${baseUrl("pay-api")}/pay-api"
  private val logDebug = (msg: String) => Logger.debug(s"[PayApiConnector] $msg")
  private val logError = (msg: String) => Logger.error(s"[PayApiConnector] $msg")

  def createPayment(request: CreatePaymentRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CreatePaymentResponse]] = {

    val bodyParser = JsonParsed[CreatePaymentResponse]

    logDebug(s"Creating payment: ${Json.toJson(request)}")
    http.POST[CreatePaymentRequest, HttpResponse](s"$payBaseUrl/amls/journey/start", request) map {
      case response & bodyParser(JsSuccess(body: CreatePaymentResponse, _)) =>
        auditConnector.sendExtendedEvent(CreatePaymentEvent(request, body))
        body.some

      case response: HttpResponse =>
        auditConnector.sendExtendedEvent(CreatePaymentFailureEvent(request.reference, response.status, response.body, request))
        logError(s"${request.reference}, status: ${response.status}: Failed to create payment using pay-api, reverting to old payments page")
        None
    }
  }

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
