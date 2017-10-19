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

package utils

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.Crypto
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

import scala.concurrent.Future
import uk.gov.hmrc.http.SessionKeys

trait AuthorisedFixture extends MockitoSugar {

  val authConnector = mock[AuthConnector]

  val authority = Authority(
    "Test User",
    Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Strong ,ConfidenceLevel.L50, None, None,None, ""
  )

  implicit val authRequest = FakeRequest().withSession(
    SessionKeys.sessionId -> "SessionId",
    SessionKeys.token -> "Token",
    SessionKeys.userId -> "Test User",
    SessionKeys.authToken -> ""
  )
  when(authConnector.currentAuthority(any(), any())) thenReturn Future.successful(Some(authority))
}




