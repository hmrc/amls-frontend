package controllers

import java.util.UUID

import _root_.builders.AuthBuilder
import _root_.builders.SessionBuilder
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.http.{Status => HttpStatus}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AmlsService
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{CtAccount, Accounts, Authority}
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.Future

class AmlsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  implicit val request = FakeRequest()
  val fakePostRequest = FakeRequest("POST", "/login").withFormUrlEncodedBody(
    "name" -> "test",
    "password" -> "password"
  )
  val mockAmlsService = mock[AmlsService]
  val mockAuthConnector = mock[AuthConnector]

  object MockAmlsController extends AmlsController {
     val authConnector = mockAuthConnector
     val amlsService: AmlsService = mockAmlsService
  }

  "AmlsController" must {

    "Authorised users" must {

      "load the Sample Login page" in {
        getWithAuthorisedUser {
          result =>
            status(result) must be(HttpStatus.OK)
            contentAsString(result) must include("name")
        }
      }
      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }
      }
      "successfully submit to Micro Service" in {
        implicit val request = fakePostRequest
        when(mockAmlsService.submitLoginDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(HttpStatus.OK,
          Some(Json.parse( """{"foo":"bar"}""")))))
        val result = MockAmlsController.onSubmit.apply(request)
        status(result) must be(HttpStatus.OK)
        contentAsString(result) must include("foo")

      }

      "fail test when Micro Service throws exception" in {
        implicit val request = fakePostRequest
        when(mockAmlsService.submitLoginDetails(Matchers.any())(Matchers.any())).thenReturn(Future.failed(new RuntimeException("test")))
        val result = MockAmlsController.onSubmit.apply(request)
        status(result) must be(HttpStatus.BAD_REQUEST)
      }
    }

  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
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
}

