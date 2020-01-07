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

import javax.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatorConnector @Inject()(http: HttpPost,
                                       environment: Environment,
                                       val runModeConfiguration: Configuration) extends ServicesConfig {

  val serviceUrl = baseUrl("government-gateway-authentication")

  def refreshProfile(implicit hc: HeaderCarrier, ec: ExecutionContext) = {

    //noinspection SimplifyBooleanMatch
    getConfBool("feature-toggle.refresh-profile", defBool = false) match {
      case true =>
        http.POSTEmpty(s"$serviceUrl/government-gateway-authentication/refresh-profile") map { response =>
          Logger.info("[AuthenticatorConnector] Current user profile was refreshed")
          response
        }
      case _ => Future.successful(HttpResponse(OK))
    }

  }

  override protected def mode: Mode = environment.mode
}
