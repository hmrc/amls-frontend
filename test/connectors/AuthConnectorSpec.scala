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

package connectors

import config.{AppConfig, WSHttp}
import models.auth.UserDetails
import models.enrolment.GovernmentGatewayEnrolment
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext

class AuthConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

    when(authContext.userDetailsUri) thenReturn Some("/user-details")

    val config = mock[AppConfig]
    when(config.authUrl) thenReturn "/auth"

    val authConnector = new AuthConnector(mock[WSHttp], config)

    val completeAuthorityModel = Authority("/", Accounts(), "/details", "/one/two/three", "12345678")
  }

  "Auth Connector" must {
    "return list of government gateway enrolments" in new Fixture {
      when(authConnector.http.GET[List[GovernmentGatewayEnrolment]](any())(any(), any(), any())).thenReturn(Future.successful(Nil))

      whenReady(authConnector.enrolments("thing")) {
        results => results must equal(Nil)
      }
    }

    "get the current authority" in new Fixture {
      when {
        authConnector.http.GET[Authority](any())(any(), any(), any())
      } thenReturn Future.successful(completeAuthorityModel)

      whenReady(authConnector.getCurrentAuthority) {
        _ mustBe completeAuthorityModel
      }
    }

    "return a failed Future when the HTTP call is unauthorised" in new Fixture {
      when {
        authConnector.http.GET[Authority](any())(any(), any(), any())
      } thenReturn Future.failed(Upstream4xxResponse("Unauthorized", UNAUTHORIZED, UNAUTHORIZED))

      intercept[Exception] {
        await(authConnector.getCurrentAuthority)
      }
    }

    "get the Ids" in new Fixture {
      when {
        authConnector.http.GET[Ids](any())(any(), any(), any())
      } thenReturn Future.successful(mock[Ids])

      whenReady(authConnector.getIds(completeAuthorityModel)) { _ =>
        verify(authConnector.http).GET[Ids](eqTo(s"/auth/${completeAuthorityModel.normalisedIds}"))(any(), any(), any())
      }
    }
  }

  "the userDetails method" when {
    "called with a valid uri" must {
      "return the user details response data" in new Fixture {
        when {
          authConnector.http.GET[UserDetails](any())(any(), any(), any())
        } thenReturn Future.successful(mock[UserDetails])

        whenReady(authConnector.userDetails) { _ =>
          verify(authConnector.http).GET[UserDetails](eqTo("/user-details"))(any(), any(), any())
        }
      }
    }

    "called with a missing user details uri" must {
      "throw an exception" in new Fixture {
        when(authContext.userDetailsUri) thenReturn None

        intercept[Exception] {
          await(authConnector.userDetails)
        }
      }
    }
  }
}
