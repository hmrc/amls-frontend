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

package services

import java.net.URLEncoder

import generators.auth.UserDetailsGenerator
import models.auth.{CredentialRole, UserDetailsResponse}
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.Results.{Ok, Redirect}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthServiceSpec extends PlaySpec with MustMatchers with ScalaFutures with MockitoSugar with UserDetailsGenerator with OneAppPerSuite {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.amls-frontend.public.host" -> "localhost:9222")
    .configure("microservice.services.logout.url" -> "http://logout")
    .build()

  trait Fixture {

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val authConnector = mock[AuthConnector]
    val service = new AuthService(authConnector)

    def setupUserDetails(role: String) = {
      when {
        authConnector.getUserDetails[UserDetailsResponse](any())(any(), any())
      } thenReturn Future.successful(userDetailsGen.sample.get.copy(credentialRole = role))
    }
  }

  "The auth service" when {
    "the user has a credential role of User" must {
      "execute the specified block normally" in new Fixture {
        setupUserDetails(CredentialRole.User)
        whenReady(service.validateCredentialRole()(Future.successful(Ok))) { _ mustBe Ok }
      }
    }

    "the user has a credential role of Assistant" must {
      "return a redirect result to the sign out page, with a continue url" in new Fixture {
        setupUserDetails(CredentialRole.Assistant)
        whenReady(service.validateCredentialRole()(Future.successful(Ok))) { result =>
          result mustBe Redirect(s"http://logout?continue=${URLEncoder.encode("http://localhost:9222/anti-money-laundering/unauthorised", "utf-8")}")
        }
      }
    }
  }

}
