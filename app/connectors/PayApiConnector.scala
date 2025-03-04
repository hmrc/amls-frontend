/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class PayApiConnector @Inject() (
  http: HttpClientV2,
  val auditConnector: DefaultAuditConnector,
  val applicationConfig: ApplicationConfig
) extends HttpResponseHelper
    with Logging {

  // $COVERAGE-OFF$
  private val logDebug = (msg: String) => logger.debug(s"[PayApiConnector] $msg")
  private val logWarn  = (msg: String) => logger.warn(s"[PayApiConnector] $msg")
  // $COVERAGE-ON$

  def createPayment(
    request: CreatePaymentRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CreatePaymentResponse]] = {

    val bodyParser = JsonParsed[CreatePaymentResponse]
    // $COVERAGE-OFF$
    logDebug(s"Creating payment: ${Json.toJson(request)}")
    // $COVERAGE-ON$
    http
      .post(url"${applicationConfig.payBaseUrl}/amls/journey/start")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map {
        case response & bodyParser(JsSuccess(body: CreatePaymentResponse, _)) =>
          auditConnector.sendExtendedEvent(CreatePaymentEvent(request, body))
          body.some

        case response: HttpResponse =>
          auditConnector.sendExtendedEvent(
            CreatePaymentFailureEvent(request.reference, response.status, response.body, request)
          )
          // $COVERAGE-OFF$
          logWarn(
            s"${request.reference}, status: ${response.status}: Failed to create payment using pay-api, reverting to old payments page"
          )
          // $COVERAGE-ON$
          None
      }
  }
}
