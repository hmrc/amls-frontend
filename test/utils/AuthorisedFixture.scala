package utils

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

trait AuthorisedFixture extends MockitoSugar {

  val authConnector = mock[AuthConnector]

  val authority = Authority(
    "Test User",
    Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Strong ,ConfidenceLevel.L50, None, None
  )

  implicit val request = FakeRequest().withSession(
    SessionKeys.sessionId -> "SessionId",
    SessionKeys.token -> "Token",
    SessionKeys.userId -> "Test User"
  )

  when(authConnector.currentAuthority(any())) thenReturn Future.successful(Some(authority))
}
