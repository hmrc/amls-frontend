package builders

import java.util.UUID

import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContent}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.http.SessionKeys

object SessionBuilder {



  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContent], userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  def buildRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  def buildRequestWithSessionNoUser() = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId)
  }
}
