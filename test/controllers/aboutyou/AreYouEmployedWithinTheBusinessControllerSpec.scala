package controllers.aboutyou

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import connectors.DataCacheConnector
import forms.AreYouEmployedWithinTheBusinessForms._
import models.{AreYouEmployedWithinTheBusinessModel, LoginDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class AreYouEmployedWithinTheBusinessControllerSpec extends PlaySpec with OneServerPerSuite with Actions with MockitoSugar {

  implicit val request = FakeRequest()
  val userId = s"user-${UUID.randomUUID}"
  val areYouEmployedWithinTheBusinessModel = AreYouEmployedWithinTheBusinessModel("Yes")
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockAreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
    override val authConnector = mockAuthConnector
    override val dataCacheConnector = mockDataCacheConnector
  }

  val fakePostRequest = FakeRequest("POST", "/about-you-2").withFormUrlEncodedBody("radio-inline" -> "Yes")
  val loginDtls = LoginDetails("testuser", "password")

  //For Loading the Page
  "On Page load" must {

    "Authorised users" must {
      "load the Are You Employed Within the Business" in {
        getWithAuthorisedUser {
          futureResult => status(futureResult) must be(OK)
            contentAsString(futureResult) must include(Messages("areYouEmployedWithinTheBusiness.lbl.areyouemployed"))
        }
      }

      "load Your Name page with pre populated data" in {
        getWithAuthorisedUserReturnsModel {
          result =>
            status(result) must be(OK)
            contentAsString(result) must include(Messages("areYouEmployedWithinTheBusiness.lbl.areyouemployed")) // TODO need to replace lable with the actual value
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


  //For Submitting the Page
  "On Page submit" must {
    "Authorised users" must {
      "successfully navigate to next page " in {
        submitWithAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "get validation exception if the field is not supplied" in {
        submitWithAuthorisedUserWithLengthValidation { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "get error message when the user not filled on the mandatory fields" in {
        submitWithAuthorisedUserWithoutMandatoryFields { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }

  override protected def authConnector = mockAuthConnector

  def getWithAuthorisedUser(futureResult: Future[Result] => Any) {
    /*
        implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        val result = MockAreYouEmployedWithinTheBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
        future(result)
    */
    getMockAuthorisedUser(futureResult, false)

  }

  def getWithAuthorisedUserReturnsModel(futureResult: Future[Result] => Any) {
    getMockAuthorisedUser(futureResult, true)
  }

  def getMockAuthorisedUser(test: Future[Result] => Any, isModel: Boolean) {
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    if (isModel) {
      when(mockDataCacheConnector.fetchDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))
    } else {
      when(mockDataCacheConnector.fetchDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    }
    val result = MockAreYouEmployedWithinTheBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def getWithUnAuthorisedUser(future: Future[Result] => Any) {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = MockAreYouEmployedWithinTheBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
    future(result)
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


  def submitWithAuthorisedUserWithLengthValidation(test: Future[Result] => Any) {
    createAreYouEmployedWithBusinessFormForSubmission(test, "Yes")
  }

  def submitWithAuthorisedUserWithoutMandatoryFields(test: Future[Result] => Any) {
    createAreYouEmployedWithBusinessFormForSubmission(test, "No")
  }

  def createAreYouEmployedWithBusinessFormForSubmission(test: Future[Result] => Any, yesNo: String) {
    val areYouEmployedWithinTheBusinessModel = AreYouEmployedWithinTheBusinessModel(yesNo)
    val form = areYouEmployedWithinTheBusinessForm.fill(areYouEmployedWithinTheBusinessModel)
    val fakePostRequest = FakeRequest("POST", "/your-name").withFormUrlEncodedBody(form.data.toSeq: _*)
    getAuthorisedUserWithPost(test, fakePostRequest)
  }

  def getAuthorisedUserWithPost(test: Future[Result] => Any, fakePostRequest: FakeRequest[AnyContentAsFormUrlEncoded]) {
    implicit val request = fakePostRequest
    val sessionId = s"session-${UUID.randomUUID}"
    val session = request.withSession(SessionKeys.sessionId -> sessionId,
      SessionKeys.token -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
    implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.saveDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](Matchers.any(),
      Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))
    val result = MockAreYouEmployedWithinTheBusinessController.onSubmit.apply(session)
    test(result)
  }

}
