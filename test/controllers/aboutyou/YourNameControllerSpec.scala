package controllers.aboutyou

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
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class YourNameControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

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
        "load Your Name page" in {
            when(mockDataCacheConnector.fetchDataShortLivedCache[YourName](Matchers.any())
            (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(None))
            val result = MockYourNameController.get(mock[AuthContext], request)
            status(result) must be(OK)
            contentAsString(result) must include(Messages("lbl.first_name"))
          }

        "load Your Name page with pre populated data" in {
          when(mockDataCacheConnector.fetchDataShortLivedCache[YourName](Matchers.any())
            (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(Some(yourName)))
          val result = MockYourNameController.get(mock[AuthContext], request)
          status(result) must be(OK)
          contentAsString(result) must include("firstName")
        }
      }

    "on submit of Your Name page" must {
      "successfully navigate to next page " in {
        submitWithFormFilled { result =>
          status(result) must be(SEE_OTHER)
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

    def createYourNameFormForSubmission(test: Future[Result] => Any, firstName: String, middleName:String, lastName:String) {
      val yourNameModel = YourName(firstName, Option(middleName),lastName)
      val form  = yourNameForm.fill(yourNameModel)
      val fakePostRequest = FakeRequest("POST", "/your-name").withFormUrlEncodedBody(form.data.toSeq: _*)
      when(mockDataCacheConnector.saveDataShortLivedCache[YourName](Matchers.any(), Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(yourName)))
      val result = MockYourNameController.post(mock[AuthContext], fakePostRequest)
    }

    def submitWithFormFilled(test: Future[Result] => Any) {
      createYourNameFormForSubmission(test, "firt name","middle name","last name")
    }

    def submitWithLengthValidation(test: Future[Result] => Any) {
      createYourNameFormForSubmission(test, "test test test test test test test test test test test test test test test test test test test",
        "","test test test test test test test test test test test test test test test test test test test")
    }

    def submitWithoutMandatoryFields(test: Future[Result] => Any) {
      createYourNameFormForSubmission(test,"","","")
    }

  }
}
