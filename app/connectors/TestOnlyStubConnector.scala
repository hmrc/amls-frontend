/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext

class TestOnlyStubConnector @Inject()(val http: HttpClient,
                                      applicationConfig: ApplicationConfig,
                                      val configuration: Configuration) extends ServicesConfig(configuration) {

  lazy val baseUrl = applicationConfig.testOnlyStubsUrl

  def clearState()(implicit hc: HeaderCarrier, ex: ExecutionContext) = {
    val requestUrl = s"$baseUrl/clearstate"
    http.DELETE[HttpResponse](requestUrl)
  }
}
