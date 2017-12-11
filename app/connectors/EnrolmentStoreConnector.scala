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

import config.{AppConfig, WSHttp}
import models.enrolment.{Constants, ESEnrolment}
import models.enrolment.Formatters._
import okhttp3.HttpUrl
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreConnector @Inject()(http: WSHttp, appConfig: AppConfig) {

  lazy val baseUrl = appConfig.config.baseUrl("tax-enrolments")

}
