package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessCustomerService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class RegisteredOfficeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {
  private val registeredAddress = BCAddress("line_1", "line_2", Some(""), Some(""), Some("CA3 9ST"), "UK")
  val businessCustomerDetails = BusinessCustomerDetails("businessName", Some("businessType"),
    registeredAddress, "sapNumber", "safeId", Some("agentReferenceNumber"), Some("firstName"), Some("lastName"))
  private val registeredOffice = RegisteredOffice(isRegisteredOffice=true, isCorrespondenceAddressSame=false)
  private val registeredOfficeSave4Later = RegisteredOfficeSave4Later(registeredAddress, isRegisteredOffice=true, isCorrespondenceAddressSame=false)
  private implicit val authContext = mock[AuthContext]
  private val mockAuthConnector = mock[AuthConnector]
  private val mockDataCacheConnector = mock[DataCacheConnector]
  private val mockBusinessCustomerService = mock[BusinessCustomerService]
  private val EndpointURL = "/registered-office"

  "The Page load" must {
    implicit val fakeGetRequest = FakeRequest()
    implicit val headerCarrier = mock[HeaderCarrier]

    "throws exception if the Registered Office page does not find the Address" in {
      a[java.lang.RuntimeException] should be thrownBy {
        when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]).
          thenThrow(new RuntimeException)
        when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        MockRegisteredOfficeController.get
      }
    }

    "displays the Registered Office page with Address" in {
      when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
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
      when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](any(), any())).
        thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(registeredOffice)))
      val futureResult = MockRegisteredOfficeController.get
      status(futureResult) must be(OK)
      val optionTuple: Option[String] = RegisteredOffice.unapplyString(registeredOffice)
      optionTuple.getOrElse("") must be("1")
    }
  }

  "Page post" must {
    implicit val headerCarrier = mock[HeaderCarrier]

    "when valid values entered forward to NotImplemented page" in {
      import forms.AboutTheBusinessForms.registeredOfficeForm
      val registeredOfficeForm1 = registeredOfficeForm.fill(registeredOffice)
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL).withFormUrlEncodedBody(registeredOfficeForm1.data.toSeq: _*)

      when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](any(), any())).
        thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.saveDataShortLivedCache[RegisteredOfficeSave4Later](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(registeredOfficeSave4Later)))
      val futureResult = MockRegisteredOfficeController.post
      status(futureResult) must be(SEE_OTHER)
      redirectLocation(futureResult).fold("") { x => x } must include ("/telephoning-business")
    }

    "display validation message when no radio button chosen" in {
      implicit val fakePostRequest = FakeRequest("POST", EndpointURL).withFormUrlEncodedBody(("isRegisteredOffice", ""))

      when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](any(), any())).
        thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.saveDataShortLivedCache[RegisteredOffice](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(registeredOffice)))

      val futureResult = MockRegisteredOfficeController.post
      status(futureResult) must be(BAD_REQUEST)
    }

  }

  object MockRegisteredOfficeController extends RegisteredOfficeController {
    override def businessCustomerService: BusinessCustomerService = mockBusinessCustomerService

    def authConnector = mockAuthConnector

    override def dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

}
