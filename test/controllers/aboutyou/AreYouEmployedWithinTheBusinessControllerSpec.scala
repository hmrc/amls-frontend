package controllers.aboutyou

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import connectors.DataCacheConnector
import models.LoginDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AreYouEmployedWithinTheBusinessService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class AreYouEmployedWithinTheBusinessControllerSpec extends PlaySpec with OneServerPerSuite with Actions with MockitoSugar {

  implicit val request = FakeRequest()
  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockAreYouEmployedWithinTheBusinessService = mock[AreYouEmployedWithinTheBusinessService]

  object MockAreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
    val authConnector = mockAuthConnector
    val areYouEmployedWithinTheBusinessService = mockAreYouEmployedWithinTheBusinessService
    val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  val fakePostRequest = FakeRequest("POST", "/about-you-2").withFormUrlEncodedBody("radio-inline" -> "Yes")
  val loginDtls: LoginDetails = LoginDetails("testuser", "password")

  "On Page load" must {

    "Authorised users" must {
      "load the Are You Employed Within the Business" in {
        getWithAuthorisedUser {
          futureResult => status(futureResult) must be(OK)
        }
      }
    }

    "UnAuthorised users" must {
      "Be redirected to the login Page" in {
        getWithUnAuthorisedUser { futureResult =>
          status(futureResult) must be(SEE_OTHER)
          redirectLocation(futureResult).fold("") { x => x } must include("/unauthorised")
        }
      }
    }
  }

  override protected def authConnector: AuthConnector = mockAuthConnector

  def getWithAuthorisedUser(futureResult: Future[Result] => Any) {
    getMockAuthorisedUser
    val result = MockAreYouEmployedWithinTheBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    futureResult(result)

    def getMockAuthorisedUser() {
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    }
  }


  def getWithUnAuthorisedUser(futureResult: Future[Result] => Any) {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = MockAreYouEmployedWithinTheBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    futureResult(result)
  }


  def submitWithAuthorisedUser(test: Future[Result] => Any) {
    implicit val request = fakePostRequest
    val sessionId = s"session-${UUID.randomUUID}"
    val session = request.withSession(SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.saveDataShortLivedCache[LoginDetails](Matchers.any(),
      Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(loginDtls)))
    val result = MockAreYouEmployedWithinTheBusinessController.onSubmit.apply(session)
    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = MockAreYouEmployedWithinTheBusinessController.onSubmit.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
