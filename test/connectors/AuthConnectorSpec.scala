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

class AuthConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    implicit val headerCarrier = HeaderCarrier()

    object TestAuthConnector extends AuthConnector {
      override private[connectors] def authUrl: String = "/auth"
      override private[connectors] val http = mock[CoreGet]
    }

    val completeAuthorityModel = Authority("/", Accounts(), "/details", "/one/two/three")
  }

  "Auth Connector" must {
    "return list of government gateway enrolments" in new Fixture {
      when(TestAuthConnector.http.GET[List[GovernmentGatewayEnrolment]](any())(any(), any(), any())).thenReturn(Future.successful(Nil))

      whenReady(TestAuthConnector.enrollments("thing")) {
        results => results must equal(Nil)
      }
    }

    "get the current authority" in new Fixture {
      when {
        TestAuthConnector.http.GET[Authority](any())(any(), any(), any())
      } thenReturn Future.successful(completeAuthorityModel)

      whenReady(TestAuthConnector.getCurrentAuthority) { _ mustBe completeAuthorityModel }
    }

    "return a failed Future when the HTTP call is unauthorised" in new Fixture {
      when {
        TestAuthConnector.http.GET[Authority](any())(any(), any(), any())
      } thenReturn Future.failed(Upstream4xxResponse("Unauthorized", UNAUTHORIZED, UNAUTHORIZED))

      intercept[Exception] {
        await(TestAuthConnector.getCurrentAuthority)
      }
    }

    "get the Ids" in new Fixture {
      when {
        TestAuthConnector.http.GET[Ids](any())(any(), any(), any())
      } thenReturn Future.successful(mock[Ids])

      whenReady(TestAuthConnector.getIds(completeAuthorityModel)) { _ =>
        verify(TestAuthConnector.http).GET[Ids](eqTo(s"/auth/${completeAuthorityModel.normalisedIds}"))(any(), any(), any())
      }
    }
  }
}
