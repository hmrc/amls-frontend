package controllers.aboutthebusiness

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.AboutTheBusinessForms._
import models.RegisteredWithHMRCBefore
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

class BusinessRegisteredWithHMRCBeforeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  implicit val authContext = mock[AuthContext]

  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val VAT_URL = "/business-details/vat"
  val mockMLRModel = RegisteredWithHMRCBefore(true, Some("12345678"))

  object MockRegisteredForMLRController extends BusinessRegisteredWithHMRCBeforeController {
    val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "HaveYouRegForMLRBeforeController" must {
    "use correct service" in {
      BusinessRegisteredWithHMRCBeforeController.authConnector must be(AMLSAuthConnector)
    }

    "on load display the registered for MLR page" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredWithHMRCBefore](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(None))
      val result = MockRegisteredForMLRController.get
      status(result) must be(OK)
      contentAsString(result) must include(Messages("aboutthebusiness.registeredformlr.title"))
    }

    "on load display the registered for MLR  page with pre-populated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredWithHMRCBefore](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(Some(mockMLRModel)))
      val result = MockRegisteredForMLRController.get
      status(result) must be(OK)
      contentAsString(result) must include("12345678")
    }


    "on submit of page" must {
      "successfully navigate to next page " in {
        submitWithFormFilled { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).fold("") {identity} must include(VAT_URL)
        }
      }

      "successfully navigate to next page when form filled with 15 digit mlr number" in {
        submitWithFormFilledWithValidMaxLength { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).fold("") {identity} must include(VAT_URL)
        }
      }

      "successfully navigate to next page with Option Yes and optional text" in {
        submitWithYesAndOptionalText { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).fold("") {identity} must include(VAT_URL)
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
        submitWithMandatoryFileldOptionNo { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).fold("") {identity} must include(VAT_URL)
        }
      }

      "get error message when the user selected option 03 and filled invalid mandatory fields" in {
        submitWithMandatoryFieldsWithInvalidData { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    def createRegisteredForMLRForSubmission(test: Future[Result] => Any, registeredWithHMR: Boolean,
                                                  mlrNumber: Option[String]) {
      val mockRegisteredForMLR = RegisteredWithHMRCBefore(registeredWithHMR, mlrNumber)
      val form  = registeredWithHMRCBeforeForm.fill(mockRegisteredForMLR)
      val fakePostRequest = FakeRequest("POST", VAT_URL).withFormUrlEncodedBody(form.data.toSeq: _*)
      when(mockDataCacheConnector.saveDataShortLivedCache[RegisteredWithHMRCBefore](Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(mockRegisteredForMLR)))
      val result = MockRegisteredForMLRController.post(mock[AuthContext], fakePostRequest)
      test(result)
    }

    def submitWithFormFilled(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, true, Some("12345678"))
    }

    def submitWithFormFilledWithValidMaxLength(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, true, Some("123456781234567"))
    }

    def submitWithLengthValidation(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, true, Some("12"*10))
    }

    def submitWithInvalidDataValidation(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, true, Some("test"))
    }

    def submitWithMandatoryFileldOptionNo(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, false, None)
    }

    def submitWithMandatoryFieldsWithInvalidData(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, false, Some("123456789789456"))
    }

    def submitWithYesAndOptionalText(test: Future[Result] => Any) {
      createRegisteredForMLRForSubmission(test, false, None)
    }
  }
}
