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

package connectors

import javax.inject.Inject

import config.ApplicationConfig
import models.payments.{CreatePaymentRequest, CreatePaymentResponse}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}

import scala.concurrent.{ExecutionContext, Future}

class PayApiConnector @Inject()(httpPost: HttpPost, config: ServicesConfig) {

  lazy val baseUrl = s"${config.baseUrl("pay-api")}/pay-api"
  private val log = (msg: String) => Logger.debug(s"[PayApiConnector] $msg")
  
  def createPayment(request: CreatePaymentRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CreatePaymentResponse]] = {
    if (config.getConfBool(ApplicationConfig.paymentsUrlLookupToggleName, defBool = false)) {
      log(s"Creating payment: ${Json.toJson(request)}")
      httpPost.POST[CreatePaymentRequest, CreatePaymentResponse](s"$baseUrl/payment", request) map { r => Some(r) }
    } else {
      Future.successful(None)
    }
  }

}
