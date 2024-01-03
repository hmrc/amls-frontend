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
import uk.gov.hmrc.http.{HeaderCarrier}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.ExecutionContext

class EnrolmentStubConnector @Inject()(http: HttpClient, config: ApplicationConfig) {

  lazy val baseUrl = config.enrolmentStubsUrl

  def enrolments(groupId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {
    val requestUrl = s"$baseUrl/auth/oid/$groupId/enrolments"
    http.GET[Seq[GovernmentGatewayEnrolment]](requestUrl)
  }
}
