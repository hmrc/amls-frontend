package controllers

import java.util.UUID

import _root_.builders.AuthBuilder
import _root_.builders.SessionBuilder
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.LoginDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AmlsService
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{SessionKeys, HttpResponse}

import scala.concurrent.Future

class AmlsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  val fakePostRequest = FakeRequest("POST", "/login").withFormUrlEncodedBody(
    "name" -> "test",
    "password" -> "password"
  )
  val loginDtls: LoginDetails = LoginDetails("testuser", "password")
  val mockAmlsService = mock[AmlsService]
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockAmlsController extends AmlsController {
    val authConnector = mockAuthConnector
    val amlsService: AmlsService = mockAmlsService
    val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "AmlsController" must {

    "use the correct Service" in {
      AmlsController.amlsService must be(AmlsService)
      AmlsController.authConnector must be(AMLSAuthConnector)
    }

    "on load of page " must {
      "Authorised users" must {
        "must load the Sample Login page" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              contentAsString(result) must include("name")
          }
        }

        "should prepopulate Login page from save4Later" in {
          getAuthorisedUserWithSave4Later {
            result =>
              status(result) must be(OK)
              contentAsString(result) must include("testuser")
          }
        }

        /*"should throw Exception" in {
           {


             val thrown =  intercept[RuntimeException]{getAuthorisedUserWithException {
             result =>
              }

             }
          }
        }*/
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).fold("") {x=>x} must include("/unauthorised")
            //redirectLocation(result).get must include("/unauthorised")
          }
        }
      }

      "unauthenticated users" must {
        "respond with a redirect" in {
          getWithUnAuthenticated { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).fold("") {x=>x} must include("/account/sign-in")
          }
        }
      }
    }

    "on submit" must {
      "Authorised users" must {
        "successfully submit to Micro Service" in {
          submitWithAuthorisedUser { result =>
            status(result) must be(OK)
            contentAsString(result) must include("foo")
          }
        }

        "fail test when Micro Service throws exception" in {
          val userId = s"user-${UUID.randomUUID}"
          implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
          AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          when(mockAmlsService.submitLoginDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new RuntimeException("test")))
          val result = MockAmlsController.onSubmit.apply(SessionBuilder.buildRequestWithSession(userId))
          status(result) must be(BAD_REQUEST)
        }
      }
      "unauthorised users" must {
        "respond with a redirect" in {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).fold("") {x=>x} must include("/unauthorised")
          }
        }
      }
    }

    "unauthorised" must {

      "respond with an OK" in {
        val result = MockAmlsController.unauthorised().apply(FakeRequest())
        status(result) must equal(OK)
      }

      "load the unauthorised page" in {
        val result = MockAmlsController.unauthorised().apply(FakeRequest())
        contentAsString(result) must include("Unauthorised")
      }
    }
  }

  def getAuthorisedUserWithSave4Later(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchDataShortLivedCache[LoginDetails](Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(loginDtls)))
    val result = MockAmlsController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getAuthorisedUserWithException(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchDataShortLivedCache[LoginDetails](Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.failed(new RuntimeException))
    val result = MockAmlsController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchDataShortLivedCache[LoginDetails](Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = MockAmlsController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = MockAmlsController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = MockAmlsController.onPageLoad.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def submitWithAuthorisedUser(test: Future[Result] => Any) {

    val userId = s"user-${UUID.randomUUID}"
    implicit val request = fakePostRequest
    val sessionId = s"session-${UUID.randomUUID}"
    val session = request.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.saveDataShortLivedCache[LoginDetails](Matchers.any(), Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(loginDtls)))
    when(mockAmlsService.submitLoginDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK,
      Some(Json.parse( """{"foo":"bar"}""")))))
    val result = MockAmlsController.onSubmit.apply(session)
    test(result)

  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = MockAmlsController.onSubmit.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}

