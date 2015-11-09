package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.aboutYou.YourNameController
import models.YourName
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class YourNameControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val userId = s"user-${UUID.randomUUID}"
  implicit val request = FakeRequest()
  val fakePostRequest = FakeRequest("POST", "/your-name").withFormUrlEncodedBody(
    "firtname" -> "first name",
    "middlename" -> "middle name",
    "lastname" -> "last name"
  )
  val yourName: YourName = YourName("FirstName", "middleName", "lastName")
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockYourNameController extends YourNameController {
    override protected def authConnector: AuthConnector = mockAuthConnector
    val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  "AboutYouController" must {
    "use correct service" in {
      YourNameController.authConnector must be(AMLSAuthConnector)
    }

    "on load of page " must {
      "Authorised users" must {
        "load the Sample Login page" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              contentAsString(result) must include(Messages("lbl.first_name"))
          }
        }
      }
    }

    "on submit" must {
      "Authorised users" must {
        "successfully navigate to next page " in {
          submitWithAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)

          }
        }
      }
    }
    def getMockAuthorisedUser() {
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    }

    def getWithAuthorisedUser(test: Future[Result] => Any) {
      getMockAuthorisedUser
      when(mockDataCacheConnector.fetchDataShortLivedCache[YourName](Matchers.any(),
      Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val result = MockYourNameController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any) {
      val sessionId = s"session-${UUID.randomUUID}"
      implicit val request = fakePostRequest
      val session = request.withSession(SessionKeys.sessionId -> sessionId,
        SessionKeys.token -> "RANDOMTOKEN",
        SessionKeys.userId -> userId)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.saveDataShortLivedCache[YourName](Matchers.any(),
      Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(yourName)))
      val result = MockYourNameController.onSubmit.apply(session)
      test(result)
    }

  }
}
