package controllers.aboutthebusiness

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.AboutTheBusinessForms._
import models.RegisteredForMLR
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class HaveYouRegisteredForMLRControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  implicit val authContext = mock[AuthContext]

  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  val mockMLRModel = RegisteredForMLR.applyString((true, false), Some("12345678"), None)

  object MockRegisteredForMLRController extends  HaveYouRegForMLRBeforeController {
    val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "HaveYouRegForMLRBeforeController" must {
    "use correct service" in {
      HaveYouRegForMLRBeforeController.authConnector must be(AMLSAuthConnector)
    }

    "on load display the businessHasWebsite page" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredForMLR](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(None))
      val result = MockRegisteredForMLRController.get
      status(result) must be(OK)
      contentAsString(result) must include(Messages("title.registeredForMLR"))
    }

    "on load display the businessHasWebsite page with prepopulated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredForMLR](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(Some(mockMLRModel)))
      val result = MockRegisteredForMLRController.get
      status(result) must be(OK)
      contentAsString(result) must include("12345678")
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

      "get validation exception when user enters invalid Data" in {
        submitWithInvalidDataValidation { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
      "get error message when the user not filled on the mandatory fields" in {
        submitWithoutMandatoryFields { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    def createRegisteredForMLRForSubmission(test: Future[Result] => Any, hasMLR: (Boolean, Boolean),
                                                  mlrNumber: Option[String], prevMlrNumber: Option[String]) {
      val mockRegisteredForMLR = RegisteredForMLR.applyString(hasMLR, mlrNumber, prevMlrNumber)
      val form  = RegisteredForMLRForm.fill(mockRegisteredForMLR)
      val fakePostRequest = FakeRequest("POST", "/business-with-VAT").withFormUrlEncodedBody(form.data.toSeq: _*)
      when(mockDataCacheConnector.saveDataShortLivedCache[RegisteredForMLR](Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(mockRegisteredForMLR)))
      val result = MockRegisteredForMLRController.post(mock[AuthContext], fakePostRequest)
      test(result)
    }

    def submitWithFormFilled(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, (true, false), Some("12345678"), None)
    }

    def submitWithLengthValidation(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, (true, false), Some("12"*10), None)
    }

    def submitWithInvalidDataValidation(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, (true, false), Some("sadfhasfd"), None)
    }

    def submitWithoutMandatoryFields(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, (false, false), None, None)
    }
  }
}
