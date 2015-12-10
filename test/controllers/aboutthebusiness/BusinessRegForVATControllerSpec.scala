package controllers.aboutthebusiness

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.AboutTheBusinessForms._
import models.BusinessWithVAT
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

class BusinessRegForVATControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  implicit val authContext = mock[AuthContext]

  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  val mockBusinessWithVAT = BusinessWithVAT(true, Some("123456789"))

  object MockBusinessRegForVATController extends  BusinessRegForVATController {
    val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "BusinessRegForVATController" must {
    "use correct service" in {
      BusinessRegForVATController.authConnector must be(AMLSAuthConnector)
    }

    "on load display the businessHasWebsite page" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[BusinessWithVAT](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(None))
      val result = MockBusinessRegForVATController.get
      status(result) must be(OK)
      contentAsString(result) must include(Messages("aboutthebusiness.vatnumber.title"))
    }

    "on load display the businessHasWebsite page with prepopulated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[BusinessWithVAT](Matchers.any())
        (Matchers.any(), Matchers.any(),  Matchers.any())).thenReturn(Future.successful(Some(mockBusinessWithVAT)))
      val result = MockBusinessRegForVATController.get
      status(result) must be(OK)
      contentAsString(result) must include("123456789")
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

    def createBusinessRegForVATFormForSubmission(test: Future[Result] => Any, hasWebsite: Boolean,
                                                  website: Option[String]) {
      val mockBusinessHasWebsite = BusinessWithVAT(hasWebsite, website)
      val form  = businessRegForVATForm.fill(mockBusinessHasWebsite)
      val fakePostRequest = FakeRequest("POST", "/business-with-VAT").withFormUrlEncodedBody(form.data.toSeq: _*)
      when(mockDataCacheConnector.saveDataShortLivedCache[BusinessWithVAT](Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(mockBusinessHasWebsite)))
      val result = MockBusinessRegForVATController.post(mock[AuthContext], fakePostRequest)
      test(result)
    }

    def submitWithFormFilled(test: Future[Result] => Any) {
      createBusinessRegForVATFormForSubmission(test, false, Some(""))
    }

    def submitWithLengthValidation(test: Future[Result] => Any) {
      createBusinessRegForVATFormForSubmission(test, true, Some("12"*10))
    }

    def submitWithInvalidDataValidation(test: Future[Result] => Any) {
      createBusinessRegForVATFormForSubmission(test, true, Some("sadfhasfd"))
    }

    def submitWithoutMandatoryFields(test: Future[Result] => Any) {
      createBusinessRegForVATFormForSubmission(test,true,None)
    }
  }
}
