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

import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceInjectorBuilder}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AuthenticatorConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  trait TestFixture {

    val http = mock[HttpPost]

    implicit val hc = HeaderCarrier()

    val featureToggleSetting: Boolean

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure("microservice.services.feature-toggle.refresh-profile" -> featureToggleSetting)
      .overrides(bind[HttpPost].to(http))
      .build()

    lazy val connector = app.injector.instanceOf(classOf[AuthenticatorConnector])

    lazy val config = app.injector.instanceOf(classOf[ServicesConfig])

    lazy val configKey = config.baseUrl("government-gateway-authentication")
  }

  "The Authenticator connector" must {

    "connect to the authenticator service to refresh the auth profile" in new TestFixture {

      val featureToggleSetting = true

      when(http.POSTEmpty[HttpResponse](any())(any(), any())) thenReturn Future.successful(HttpResponse(200))

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http).POSTEmpty(eqTo(s"$configKey/authenticator/refresh-profile"))(any(), any())

    }

    "return a default successful result when the feature is toggled off" in new TestFixture {

      val featureToggleSetting = false

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http, never).POSTEmpty(any())(any(), any())
    }

  }

}
