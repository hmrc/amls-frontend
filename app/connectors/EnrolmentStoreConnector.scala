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
import okhttp3.HttpUrl
import play.api.http.Status._
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreConnector @Inject()(http: CoreGet, appConfig: AppConfig) {

  lazy val baseUrl = appConfig.config.baseUrl("enrolment-store")

  def userEnrolments(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ESEnrolment]] = {
    val url = EnrolmentStoreConnector.enrolmentsUrl(userId, baseUrl)
    
    http.GET[HttpResponse](url) map {
      case HttpResponse(OK, json, _, _) => json.asOpt[ESEnrolment]
      case HttpResponse(NO_CONTENT, _, _, _) => None
    }
  }

}

object EnrolmentStoreConnector {

  val serviceName = "HMRC-MLR-ORG"

  def enrolmentsUrl(userId: String, baseUrl: String) = {
    val parts = HttpUrl.parse(baseUrl)

    new HttpUrl.Builder()
      .host(parts.host)
      .scheme(parts.scheme)
      .port(parts.port)
      .addPathSegments(s"users/$userId/enrolments")
      .addQueryParameter("service", serviceName)
      .addQueryParameter("type", "principal")
      .addQueryParameter("start-record", "1")
      .addQueryParameter("max-records", "1000")
      .build()
      .url()
      .toString
  }
}
