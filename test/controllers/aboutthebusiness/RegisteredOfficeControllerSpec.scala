package controllers.aboutthebusiness

import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import forms.AboutTheBusinessForms._
import models._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class RegisteredOfficeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {
  private val bCAddress = BCAddress("line_1", "line_2", Some(""), Some(""), Some("CA3 9ST"), "UK")
  private val businessCustomerDetails = BusinessCustomerDetails("businessName", Some("businessType"),
    bCAddress, "sapNumber", "safeId", Some("agentReferenceNumber"), Some("firstName"), Some("lastName"))
  private val regOffTrueTrue = RegisteredOffice(isRegisteredOffice = true, isCorrespondenceAddressSame = true)
  private val regOffSave4LaterTrueTrue = RegisteredOfficeSave4Later(bCAddress, isRegisteredOffice = true,
    isCorrespondenceAddressSame = true)
  private implicit val authContext = mock[AuthContext]
  private val mockAuthConnector = mock[AuthConnector]
  private val mockDataCacheConnector = mock[DataCacheConnector]
  private val mockSessionCacheConnector = mock[BusinessCustomerSessionCacheConnector]
  private val EndpointURL = "/registered-office"

  "The Page load" must {
    implicit val fakeGetRequest = FakeRequest()
    implicit val headerCarrier = mock[HeaderCarrier]

    "throws exception if the Registered Office page does not find the Address" in {
      a[java.lang.RuntimeException] should be thrownBy {
        when(mockSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]).
          thenThrow(new RuntimeException)
        when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        MockRegisteredOfficeController.get
      }
    }

    "displays the Registered Office page with Address" in {
      when(mockSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
        .thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val futureResult = MockRegisteredOfficeController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_1)
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_2)
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_3.getOrElse(""))
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_4.getOrElse(""))
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.postcode.getOrElse(""))
    }


    "load the Registered Office details from the Cache" in {
      when(mockSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any())).
        thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOfficeSave4Later](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(regOffSave4LaterTrueTrue)))
      val futureResult = MockRegisteredOfficeController.get
      status(futureResult) must be(OK)
      val optionTuple: Option[String] = RegisteredOffice.unapplyString(regOffTrueTrue)
      optionTuple.getOrElse("") must be("1")
    }
  }

  "Page post" must {
    implicit val headerCarrier = mock[HeaderCarrier]

    "when valid values entered including choice of first option then forward to Telephoning Business page" in {
      val registeredOfficeForm1 = registeredOfficeForm.fill(regOffTrueTrue)
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL)
        .withFormUrlEncodedBody(registeredOfficeForm1.data.toSeq: _*)

      postFormAndTestResult(regOffSave4LaterTrueTrue, result => {
        status(result) must be(SEE_OTHER)
        redirectLocation(result).fold("") { x => x } must include("/telephoning-business")
      })
    }

    "when valid values entered including choice of second option then forward to NotImplemented page" in {
      val registeredOfficeForm1 = registeredOfficeForm.fill(RegisteredOffice(isRegisteredOffice = true,
        isCorrespondenceAddressSame = false))
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL)
        .withFormUrlEncodedBody(registeredOfficeForm1.data.toSeq: _*)

      val registeredOfficeSave4Later = RegisteredOfficeSave4Later(bCAddress, isRegisteredOffice = true,
        isCorrespondenceAddressSame = false)

      postFormAndTestResult(registeredOfficeSave4Later, result => status(result) must be(NOT_IMPLEMENTED)
      )
    }

    "display validation message when no radio button chosen" in {
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL).withFormUrlEncodedBody(("isRegisteredOffice", ""))
      postFormAndTestResult(regOffSave4LaterTrueTrue, result => status(result) must be(BAD_REQUEST)
      )
    }

  }

  private def postFormAndTestResult(registeredOfficeSave4Later: RegisteredOfficeSave4Later,
                                    test: Future[Result] => Any)(
    implicit request: FakeRequest[AnyContentAsFormUrlEncoded]) = {
    when(mockSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any())).
      thenReturn(Future.successful(businessCustomerDetails))
    when(mockDataCacheConnector.saveDataShortLivedCache[RegisteredOfficeSave4Later](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(registeredOfficeSave4Later)))
    val result = MockRegisteredOfficeController.post
    test(result)
  }

  object MockRegisteredOfficeController extends RegisteredOfficeController {
    override def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector = mockSessionCacheConnector

    def authConnector = mockAuthConnector

    override def dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }


}
