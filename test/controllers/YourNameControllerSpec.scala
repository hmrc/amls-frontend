package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.aboutYou.YourNameController
import forms.AboutYouForms._
import models.YourName
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

class YourNameControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val userId = s"user-${UUID.randomUUID}"
  implicit val request = FakeRequest()

  val yourName: YourName = YourName("firstName", Option("middleName"), "lastName")
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

    "on load of page AboutYou" must {
      "Authorised users" must {
        "load Your Name page" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              contentAsString(result) must include(Messages("lbl.first_name"))
          }
        }

        "load Your Name page with pre populated data" in {
          getWithAuthorisedUserReturnsModel {
            result =>
              status(result) must be(OK)
              contentAsString(result) must include(Messages("lbl.first_name")) // TODO need to replace lable with the actual value
          }
        }

     /*   "fail test when there is an exception" in {

         a [Exception] must be thrownBy {
           implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
           AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
           when(mockDataCacheConnector.fetchDataShortLivedCache[YourName](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.failed(new RuntimeException("test")))
           val result = MockYourNameController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
          }
         /* getMockAuthorisedUserWithException {
            result =>
             result  //TODO

          }*/
        }*/
      }
    }

    "on submit" must {
      "Authorised users" must {
        "successfully navigate to next page " in {
          submitWithAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)

          }
        }

        "get validation exception when the length of the text field is greater than 35 characters" in {
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

    def getMockAuthorisedUser(test: Future[Result] => Any, isModel: Boolean) {
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      if (isModel)
      {
        when(mockDataCacheConnector.fetchDataShortLivedCache[YourName](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(yourName)))
      } else {
        when(mockDataCacheConnector.fetchDataShortLivedCache[YourName](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      }
      val result = MockYourNameController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(test: Future[Result] => Any) {
      getMockAuthorisedUser(test, false)
    }
    def getWithAuthorisedUserReturnsModel(test: Future[Result] => Any) {
      getMockAuthorisedUser(test, true)
    }

    def getMockAuthorisedUserWithException(test: Future[Result] => Any) {
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchDataShortLivedCache[YourName](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any())).thenReturn(Future.failed(new RuntimeException("test")))
      val result = MockYourNameController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getAuthorisedUserWithPost(test: Future[Result] => Any, fakePostRequest:FakeRequest[AnyContentAsFormUrlEncoded]) {
      implicit val request = fakePostRequest
      val sessionId = s"session-${UUID.randomUUID}"
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

    def createYourNameFormForSubmission(test: Future[Result] => Any, firstName: String, middleName:String, lastName:String) {
      val yourNameModel = YourName(firstName, Option(middleName),lastName)
      val form  = yourNameForm.fill(yourNameModel)
      val fakePostRequest = FakeRequest("POST", "/your-name").withFormUrlEncodedBody(form.data.toSeq: _*)
      getAuthorisedUserWithPost(test, fakePostRequest)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any) {
      createYourNameFormForSubmission(test, "firt name","middle name","last name")
    }

    def submitWithAuthorisedUserWithLengthValidation(test: Future[Result] => Any) {
      createYourNameFormForSubmission(test, "test test test test test test test test test test test test test test test test test test test",
        "","test test test test test test test test test test test test test test test test test test test")
    }

    def submitWithAuthorisedUserWithoutMandatoryFields(test: Future[Result] => Any) {
      createYourNameFormForSubmission(test,"","","")
    }

  }
}
