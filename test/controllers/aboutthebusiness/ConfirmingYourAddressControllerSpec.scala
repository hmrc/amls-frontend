/*
package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import forms.AboutTheBusinessForms.confirmingYourAddressForm
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

class ConfirmingYourAddressControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {
  private val bCAddress = BCAddress("line_1", "line_2", Some(""), Some(""), Some("CA3 9ST"), "UK")
  private val businessCustomerDetails = BusinessCustomerDetails("businessName", Some("businessType"),
    bCAddress, "sapNumber", "safeId", Some("agentReferenceNumber"), Some("firstName"), Some("lastName"))
  private val confirmingAddress = ConfirmingYourAddress(isRegOfficeOrMainPlaceOfBusiness = true)
  private val confirmingAddressSave4Later = ConfirmingYourAddressSave4Later(bCAddress, isRegOfficeOrMainPlaceOfBusiness = true)
  private implicit val authContext = mock[AuthContext]
  private val mockAuthConnector = mock[AuthConnector]
  private val mockDataCacheConnector = mock[DataCacheConnector]
  private val mockSessionCacheConnector = mock[BusinessCustomerSessionCacheConnector]
  private val EndpointURL = "/business-details/confirming-address"

  "The Page load" must {
    implicit val fakeGetRequest = FakeRequest()
    implicit val headerCarrier = mock[HeaderCarrier]

    "use correct service" in {
      ConfirmingYourAddressController.authConnector must be(AMLSAuthConnector)
      ConfirmingYourAddressController.businessCustomerSessionCacheConnector must be(BusinessCustomerSessionCacheConnector)
      ConfirmingYourAddressController.dataCacheConnector must be(DataCacheConnector)
    }

    "throws exception if the Registered Office page does not find the Address" in {
      a[java.lang.RuntimeException] should be thrownBy {
        when(mockSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]).
          thenThrow(new RuntimeException)
        when(mockDataCacheConnector.fetchDataShortLivedCache[ConfirmingYourAddress](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        MockConfirmingYourAddressController.get
      }
    }

    "displays the Registered Office page with Address" in {
      when(mockSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
        .thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.fetchDataShortLivedCache[ConfirmingYourAddress](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val futureResult = MockConfirmingYourAddressController.get
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
      when(mockDataCacheConnector.fetchDataShortLivedCache[ConfirmingYourAddressSave4Later](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(confirmingAddressSave4Later)))
      val futureResult = MockConfirmingYourAddressController.get
      status(futureResult) must be(OK)
    }
  }

  "Page post" must {
    implicit val headerCarrier = mock[HeaderCarrier]

    "when valid values entered including choice of first option then forward to Telephoning Business page" in {
      val registeredOfficeForm1 = confirmingYourAddressForm.fill(confirmingAddress)
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL)
        .withFormUrlEncodedBody(registeredOfficeForm1.data.toSeq: _*)

      postFormAndTestResult(confirmingAddressSave4Later, result => {
        status(result) must be(SEE_OTHER)
        redirectLocation(result).fold("") { x => x } must include("/telephoning-business")
      })
    }

    "when valid values entered including choice of second option then forward to NotImplemented page" in {
      val registeredOfficeForm1 = confirmingYourAddressForm.fill(ConfirmingYourAddress(isRegOfficeOrMainPlaceOfBusiness = false))
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL)
        .withFormUrlEncodedBody(registeredOfficeForm1.data.toSeq: _*)

      val registeredOfficeSave4Later = ConfirmingYourAddressSave4Later(bCAddress, isRegOfficeOrMainPlaceOfBusiness = false)
      postFormAndTestResult(registeredOfficeSave4Later, result => status(result) must be(NOT_IMPLEMENTED))
    }

    "display validation message when no radio button chosen" in {
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL).withFormUrlEncodedBody(("isRegOfficeOrMainPlaceOfBusiness", ""))
      postFormAndTestResult(confirmingAddressSave4Later, result => status(result) must be(BAD_REQUEST)
      )
    }

  }

  private def postFormAndTestResult(confirmingYourAddressSave4Later: ConfirmingYourAddressSave4Later,
                                    test: Future[Result] => Any)(
    implicit request: FakeRequest[AnyContentAsFormUrlEncoded]) = {
    when(mockSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any())).
      thenReturn(Future.successful(businessCustomerDetails))
    when(mockDataCacheConnector.saveDataShortLivedCache[ConfirmingYourAddressSave4Later](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(confirmingYourAddressSave4Later)))
    val result = MockConfirmingYourAddressController.post
    test(result)
  }

  object MockConfirmingYourAddressController extends ConfirmingYourAddressController {
    override def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector = mockSessionCacheConnector

    def authConnector = mockAuthConnector

    override def dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }


}
*/
