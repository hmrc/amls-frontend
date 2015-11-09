package controllers.aboutyou

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import connectors.DataCacheConnector
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class AreYouEmployedWithinTheBusinessControllerSpec extends PlaySpec with OneServerPerSuite with Actions with MockitoSugar {

  implicit val request = FakeRequest()
  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockAreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
    val authConnector = mockAuthConnector
    val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }
  //val fakePostRequest = FakeRequest("POST", "/about-you-2").withFormUrlEncodedBody("radio-inline" -> "Yes")
  // val loginDtls: LoginDetails = LoginDetails("testuser", "password")

  "on load of page " must {
    "Authorised users" must {
      "load the Are You Employed Within the Business page" in {
        getWithAuthorisedUser {
          result =>
            status(result) must be(OK)
        }
      }
    }
  }

  override protected def authConnector: AuthConnector = mockAuthConnector

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    getMockAuthorisedUser
    val result = MockAreYouEmployedWithinTheBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getMockAuthorisedUser() {
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
  }

}
