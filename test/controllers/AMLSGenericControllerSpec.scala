package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{Result, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class AMLSGenericControllerSpec extends PlaySpec with MockitoSugar with  OneServerPerSuite {

  implicit val request = FakeRequest()
  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]

  object MockAMLSGenericController extends AMLSGenericController {
    override protected def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = Future.successful(Ok("test"))

    override protected def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = Future.successful(Ok("submit"))

    override protected def authConnector: AuthConnector = mockAuthConnector
  }

  "AMLSGenericController " should {
    "Authorised users" must {
      "successfully load sample page" in {
        getWithAuthorisedUser {
          result =>
            status(result) must be(OK)
            contentAsString(result) must include("test")
        }
      }
    }

    "on load of sample page unauthorised users" must {
      "respond with a redirect" in {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).fold("") {identity} must include("/unauthorised")
        }
      }
    }

    "on load of sample page unauthenticated users" must {
      "respond with a redirect" in {
        getWithUnAuthenticated { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).fold("") {identity} must include("/account/sign-in")
        }
      }
    }

    "Authorised users" must {
      "successfully submit sample page" in {
        submitWithAuthorisedUser {
          result =>
            status(result) must be(OK)
            contentAsString(result) must include("submit")
        }
      }

      "on submmit of sample page Unauthorised user" must {
        "be redirected to the next page" in {
          submitWithUnAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).fold("") {identity} must include("/unauthorised")
          }
        }
      }

      "on submit of sample page unauthenticated user" must {
        "be redirected to the next page" in {
          submitWithUnAuthenticated {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).fold("") {identity} must include("/account/sign-in")
          }
        }
      }
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any) {
      val sessionId = s"session-${UUID.randomUUID}"
      val session = request.withSession(SessionKeys.sessionId -> sessionId,
        SessionKeys.token -> "RANDOMTOKEN",
        SessionKeys.userId -> userId)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      val result = MockAMLSGenericController.post().apply(session)
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
      val result = MockAMLSGenericController.get().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthenticated(test: Future[Result] => Any) {
      val result = MockAMLSGenericController.get().apply(SessionBuilder.buildRequestWithSessionNoUser())
      test(result)
    }

    def getWithAuthorisedUser(test: Future[Result] => Any) {
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      val result = MockAMLSGenericController.get().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
      implicit val user = AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
      val result = MockAMLSGenericController.get().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithUnAuthenticated(test: Future[Result] => Any) {
      val result = MockAMLSGenericController.get().apply(SessionBuilder.buildRequestWithSessionNoUser())
      test(result)
    }
  }
}
