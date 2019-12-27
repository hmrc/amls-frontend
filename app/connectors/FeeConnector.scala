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

import config.ApplicationConfig
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class FeeConnector @Inject()(
                              private[connectors] val http: HttpClient,
                              appConfig: ApplicationConfig) {

  val feePaymentUrl = appConfig.feePaymentUrl

  def feeResponse(amlsRegistrationNumber: String, accountTypeId: (String, String))
                 (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[FeeResponse]): Future[FeeResponse] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = s"$feePaymentUrl/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[FeeConnector]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    http.GET[FeeResponse](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }
}