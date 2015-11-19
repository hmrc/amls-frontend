package controllers

import java.util.UUID
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.aboutTheBusiness.BusinessHasWebsiteController
import forms.AboutTheBusinessForms._
import models.BusinessHasWebsite
import org.mockito.Matchers

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class BusinessHasWebsiteControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  implicit val authContext = mock[AuthContext]

  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  val mockBusinessHasWebsite = BusinessHasWebsite(true, Some("waaaaaaaaaaaaaaaaaaaaaaaaaaa.com"))

  object MockBusinessHasWebsiteController extends BusinessHasWebsiteController{
    val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "BusinessHasWebsiteController" must {
    "use correct service" in {
      BusinessHasWebsiteController.authConnector must be(AMLSAuthConnector)
    }

    "on load display the businessHasWebsite page" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[BusinessHasWebsite](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(None))
      val result = MockBusinessHasWebsiteController.get
      status(result) must be(OK)
      contentAsString(result) must include("Does your business have a website?")
    }

    "on load display the businessHasWebsite page with prepopulated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[BusinessHasWebsite](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(Some(mockBusinessHasWebsite)))
      val result = MockBusinessHasWebsiteController.get
      status(result) must be(OK)
      contentAsString(result) must include("waaaaaaaaaaaaaaaaaaaaaaaaaaa.com")
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

    def createBusinessHasWebsiteFormForSubmission(test: Future[Result] => Any, hasWebsite: Boolean,
                                                  website: Option[String]) {
      val mockBusinessHasWebsite = BusinessHasWebsite(hasWebsite, website)
      val form  = businessHasWebsiteForm.fill(mockBusinessHasWebsite)
      val fakePostRequest = FakeRequest("POST", "/business-has-website").withFormUrlEncodedBody(form.data.toSeq: _*)
      when(mockDataCacheConnector.saveDataShortLivedCache[BusinessHasWebsite](Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(mockBusinessHasWebsite)))
      val result = MockBusinessHasWebsiteController.post(mock[AuthContext], fakePostRequest)
    }

    def submitWithFormFilled(test: Future[Result] => Any) {
      createBusinessHasWebsiteFormForSubmission(test, false, Some(""))
    }

    def submitWithLengthValidation(test: Future[Result] => Any) {
      createBusinessHasWebsiteFormForSubmission(test, true, Some("aaaaaaaaa" * 11))
    }

    def submitWithoutMandatoryFields(test: Future[Result] => Any) {
      createBusinessHasWebsiteFormForSubmission(test,true,None)
    }




  }
}
