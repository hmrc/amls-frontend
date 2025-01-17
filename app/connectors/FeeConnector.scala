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

import config.ApplicationConfig

import javax.inject.Inject
import models._
import play.api.Logging
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.client.HttpClientV2

class FeeConnector @Inject()(
                              http: HttpClientV2,
                              appConfig: ApplicationConfig) extends Logging {

  val feePaymentUrl = appConfig.feePaymentUrl

  def feeResponse(amlsRegistrationNumber: String, accountTypeId: (String, String))
                 (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[FeeResponse]): Future[FeeResponse] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = url"$feePaymentUrl/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[FeeConnector]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    // $COVERAGE-ON$
    http.get(getUrl).execute[FeeResponse] map {
      response =>
        // $COVERAGE-OFF$
        logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        // $COVERAGE-ON$
        response
    }
  }
}