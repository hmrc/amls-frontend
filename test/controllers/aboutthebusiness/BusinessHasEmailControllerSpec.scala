package controllers.aboutthebusiness

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.AboutTheBusinessForms._
import models.BusinessHasEmail
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

class BusinessHasEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  implicit val authContext = mock[AuthContext]

  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  val mockBusinessHasEmail = BusinessHasEmail("test@abc.com")

  object MockBusinessHasEmailController extends BusinessHasEmailController{
    val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "BusinessHasEmailController" must {
    "use correct service" in {
      BusinessHasEmailController.authConnector must be(AMLSAuthConnector)
    }

    "on load display the businessHasEmail page" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[BusinessHasEmail](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(None))
      val result = MockBusinessHasEmailController.get
      status(result) must be(OK)
      contentAsString(result) must include("Email")
    }

    "on load display the businessHasEmail page with prepopulated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[BusinessHasEmail](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(Some(mockBusinessHasEmail)))
      val result = MockBusinessHasEmailController.get
      status(result) must be(OK)
      contentAsString(result) must include("test@abc.com")
    }


    "on submit of page" must {
      "successfully navigate to next page " in {
        submitWithFormFilled { result =>
          status(result) must be(NOT_IMPLEMENTED)
        }
      }

      "get validation exception when the length of the text field is greater than MAX characters" in {
        submitWithLengthValidation { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "get error message when the user not filled on the mandatory fields" in {
        submitWithoutMandatoryFields { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    def createBusinessHasEmailFormForSubmission(test: Future[Result] => Any, email: String) {
      val mockBusinessHasEmail = BusinessHasEmail(email)
      val form  = businessHasEmailForm.fill(mockBusinessHasEmail)
      val fakePostRequest = FakeRequest("POST", "/business-has-Email").withFormUrlEncodedBody(form.data.toSeq: _*)
      when(mockDataCacheConnector.saveDataShortLivedCache[BusinessHasEmail](Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(mockBusinessHasEmail)))
      val result = MockBusinessHasEmailController.post(mock[AuthContext], fakePostRequest)
      test(result)
    }

    def submitWithFormFilled(test: Future[Result] => Any) {
      createBusinessHasEmailFormForSubmission(test, "test@google.com")
    }

    def submitWithLengthValidation(test: Future[Result] => Any) {
      createBusinessHasEmailFormForSubmission(test,"aaaaaaaaa" * 11)
    }

    def submitWithoutMandatoryFields(test: Future[Result] => Any) {
      createBusinessHasEmailFormForSubmission(test, "")
    }
  }
}
