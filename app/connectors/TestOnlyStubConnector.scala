/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext

class TestOnlyStubConnector @Inject()(val http: HttpClient,
                                      applicationConfig: AppConfig,
                                      override val runModeConfiguration: Configuration,
                                      environment: Environment) extends ServicesConfig {

  lazy val baseUrl = applicationConfig.testOnlyStubsUrl

  def clearState()(implicit hc: HeaderCarrier, ex: ExecutionContext) = {
    val requestUrl = s"$baseUrl/clearstate"
    http.DELETE[HttpResponse](requestUrl)
  }

  override protected def mode: Mode = environment.mode
}
