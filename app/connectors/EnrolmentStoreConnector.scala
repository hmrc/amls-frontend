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

import config.AppConfig
import models.enrolment.ESEnrolment
import models.enrolment.Formatters._
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

trait Enrolment

class EnrolmentStoreConnector @Inject()(http: CoreGet, appConfig: AppConfig) {

  lazy val baseUrl = appConfig.config.baseUrl("enrolment-store")

  def userEnrolments(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ESEnrolment] = {
    http.GET[ESEnrolment](s"$baseUrl/users/$userId/enrolments")
  }

}
