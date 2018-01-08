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

package services

import generators.auth.UserDetailsGenerator
import models.auth.{CredentialRole, UserDetailsResponse}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

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
        authConnector.getUserDetails[UserDetailsResponse](any())(any(), any(), any())
      } thenReturn Future.successful(userDetailsGen.sample.get.copy(credentialRole = Some(role)))
    }
  }

  "The auth service" when {
    "the user has a credential role of User" must {
      "return true" in new Fixture {
        setupUserDetails(CredentialRole.User)
        whenReady(service.validateCredentialRole) { _ mustBe true }
      }
    }

    "the user has a credential role of Assistant" must {
      "return false" in new Fixture {
        setupUserDetails(CredentialRole.Assistant)
        whenReady(service.validateCredentialRole) { _ mustBe false }
      }
    }
  }
}
