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

package connectors

import audit.{CreatePaymentEvent, CreatePaymentFailureEvent}
import cats.implicits._
import com.google.inject.Inject
import config.ApplicationConfig
import models.payments.{CreatePaymentRequest, CreatePaymentResponse}
import play.api.Logger
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, HttpClient}
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PayApiConnector @Inject()(
                                 val http: HttpClient,
                                 val auditConnector: DefaultAuditConnector,
                                 val applicationConfig: ApplicationConfig) extends HttpResponseHelper  {

  // $COVERAGE-OFF$
  private val logDebug = (msg: String) => Logger.debug(s"[PayApiConnector] $msg")
  private val logError = (msg: String) => Logger.error(s"[PayApiConnector] $msg")
  // $COVERAGE-ON$

  def createPayment(request: CreatePaymentRequest)(implicit hc: HeaderCarrier): Future[Option[CreatePaymentResponse]] = {

    val bodyParser = JsonParsed[CreatePaymentResponse]
    // $COVERAGE-OFF$
    logDebug(s"Creating payment: ${Json.toJson(request)}")
    // $COVERAGE-ON$
    http.POST[CreatePaymentRequest, HttpResponse](s"${applicationConfig.payBaseUrl}/amls/journey/start", request) map {
      case response & bodyParser(JsSuccess(body: CreatePaymentResponse, _)) =>
        auditConnector.sendExtendedEvent(CreatePaymentEvent(request, body))
        body.some

      case response: HttpResponse =>
        auditConnector.sendExtendedEvent(CreatePaymentFailureEvent(request.reference, response.status, response.body, request))
        // $COVERAGE-OFF$
        logError(s"${request.reference}, status: ${response.status}: Failed to create payment using pay-api, reverting to old payments page")
        // $COVERAGE-ON$
        None
    }
  }
}
