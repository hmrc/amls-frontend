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

import javax.inject.Inject

import config.{AppConfig, WSHttp}
import models.enrolment.{AmlsEnrolmentKey, EnrolmentKey, EnrolmentStoreEnrolment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
class EnrolmentStoreConnector @Inject()(http: WSHttp, appConfig: AppConfig, auth: AuthConnector) {

  lazy val baseUrl = s"${appConfig.enrolmentStoreUrl}/tax-enrolments"

  def enrol(enrolKey: EnrolmentKey, enrolment: EnrolmentStoreEnrolment)
           (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[HttpResponse] = {

    auth.userDetails flatMap { details =>
      val url = s"$baseUrl/groups/${details.affinityGroup}/enrolments/${enrolKey.key}"

      http.POST[EnrolmentStoreEnrolment, HttpResponse](url, enrolment)
    }
  }

}
