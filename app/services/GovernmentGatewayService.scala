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

package services

import connectors.GovernmentGatewayConnector
import models.governmentgateway.{EnrolmentRequest, EnrolmentResponse}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import play.api.http.Status.OK

import scala.concurrent.{ExecutionContext, Future}

trait GovernmentGatewayService {

  private[services] def ggConnector: GovernmentGatewayConnector

  def enrol
  (mlrRefNo: String, safeId: String)
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[HttpResponse] =
    ggConnector.enrol(EnrolmentRequest(
      mlrRefNo = mlrRefNo,
      safeId = safeId
    ))
}

object GovernmentGatewayService extends GovernmentGatewayService {
  // $COVERAGE-OFF$
  override val ggConnector = GovernmentGatewayConnector
}
