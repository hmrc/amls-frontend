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
import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatorConnector @Inject()(http: HttpClient,
                                       environment: Environment,
                                       val runModeConfiguration: Configuration,
                                       val appConfig: AppConfig) {

  def refreshProfile(implicit hc: HeaderCarrier, ec: ExecutionContext) = {

    //noinspection SimplifyBooleanMatch
    appConfig.refreshProfileToggle match {
      case true =>
        http.POSTEmpty(s"${appConfig.ggAuthUrl}/government-gateway-authentication/refresh-profile") map { response =>
          Logger.info("[AuthenticatorConnector] Current user profile was refreshed")
          response
        }
      case _ => Future.successful(HttpResponse(OK))
    }

  }
}
