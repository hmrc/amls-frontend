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
import models.enrolment.GovernmentGatewayEnrolment
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStubConnector @Inject()(http: HttpClientV2, config: ApplicationConfig) {

  lazy val baseUrl = config.enrolmentStubsUrl

  def enrolments(groupId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[GovernmentGatewayEnrolment]] = {
    val requestUrl = url"$baseUrl/auth/oid/$groupId/enrolments"
    http.get(requestUrl).execute[Seq[GovernmentGatewayEnrolment]]
  }
}
