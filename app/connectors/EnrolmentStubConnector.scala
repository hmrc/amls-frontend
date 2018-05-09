/*
 * Copyright 2018 HM Revenue & Customs
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

import config.AppConfig
import javax.inject.Inject
import models.enrolment.GovernmentGatewayEnrolment
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext

class EnrolmentStubConnector @Inject()(http: HttpGet, config: AppConfig) {

  lazy val baseUrl = config.enrolmentStubsUrl

  def enrolments(groupId: String)(implicit hc: HeaderCarrier, ac: AuthContext, ex: ExecutionContext) = {
    val requestUrl = s"$baseUrl/auth/oid/$groupId/enrolments"
    http.GET[Seq[GovernmentGatewayEnrolment]](requestUrl)
  }

}
