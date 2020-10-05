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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.{Configuration, Environment}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{HttpPost, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.http.ws.WSHttp
import utils.AmlsSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AuthenticatorConnectorSpec extends AmlsSpec with ScalaFutures {

  trait TestFixture {

    val http = mock[HttpClient]
    val appConfig = mock[ApplicationConfig]

    lazy val connector = new AuthenticatorConnector(http, appConfig)
  }

  "The Authenticator connector" must {

    "connect to the authenticator service to refresh the auth profile" in new TestFixture {

      when(appConfig.refreshProfileToggle).thenReturn(true)

      when(http.POSTEmpty[HttpResponse](any(), any())(any(), any(), any())) thenReturn Future.successful(HttpResponse(200))

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http).POSTEmpty(endsWith(s"/government-gateway-authentication/refresh-profile"), any())(any(), any(), any())

    }

    "return a default successful result when the feature is toggled off" in new TestFixture {

      when(appConfig.refreshProfileToggle).thenReturn(false)

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http, never).POSTEmpty(any(), any())(any(), any(), any())
    }

  }

}
