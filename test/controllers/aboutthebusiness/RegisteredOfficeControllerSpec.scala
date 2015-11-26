package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models._
import org.mockito.Matchers
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
  private implicit val authContext = mock[AuthContext]
  private val mockAuthConnector = mock[AuthConnector]
  private val mockDataCacheConnector = mock[DataCacheConnector]
  private val mockBusinessCustomerService = mock[BusinessCustomerService]
  private val EndpointURL = "/registered-office"
  private val registeredAddress = BCAddress("line_1", "line_2", Some(""), Some(""), Some("CA3 9ST"), "UK")

  private def registeredOfficeFormSubmissionHelper() = {
    val registeredOffice = RegisteredOffice(registeredAddress, true, false)
    implicit val fakePostRequest = FakeRequest("POST", EndpointURL).withFormUrlEncodedBody(
      ("isRegisteredOffice", "")
    )
    when(mockDataCacheConnector.saveDataShortLivedCache[RegisteredOffice](Matchers.any(),
      Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(registeredOffice)))
    MockRegisteredOfficeController.post
  }

  "The Page load" must {
    implicit val fakeGetRequest = FakeRequest()
    implicit val headerCarrier = mock[HeaderCarrier]

    val registeredOffice = RegisteredOffice(registeredAddress, true, false)
    val businessCustomerDetails = BusinessCustomerDetails("businessName", Some("businessType"),
      registeredAddress, "sapNumber", "safeId", Some("agentReferenceNumber"), Some("firstName"), Some("lastName"))

    "throws exception if the Registered Office page does not find the Address" in {
      a[java.lang.RuntimeException] should be thrownBy {
        when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]).
          thenThrow(new RuntimeException)
        when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        MockRegisteredOfficeController.get
      }
    }

    "displays the Registered Office page with Address" in {
      when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](Matchers.any(), Matchers.any())).
        thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val futureResult = MockRegisteredOfficeController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_1)
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_2)
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_3.getOrElse(""))
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.line_4.getOrElse(""))
      contentAsString(futureResult) must include(businessCustomerDetails.businessAddress.postcode.getOrElse(""))
    }


    "load the Registered Office details from the Cache" in {
      when(mockBusinessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails](Matchers.any(), Matchers.any())).
        thenReturn(Future.successful(businessCustomerDetails))
      when(mockDataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(registeredOffice)))
      val futureResult = MockRegisteredOfficeController.get
      status(futureResult) must be(OK)
      val optionTuple: Option[(BCAddress, String)] = RegisteredOffice.unapplyString(registeredOffice)
      optionTuple.map(_._2).getOrElse("") must be(s"${registeredOffice.isRegisteredOffice},${registeredOffice.isCorrespondenceAddressSame}")
    }
  }

  "Page post" must {
    "when valid values entered forward to NotImplemented page" in {

    }

    "display validation message when no radio button chosen" in {

    }

  }

  object MockRegisteredOfficeController extends RegisteredOfficeController {
    override def businessCustomerService: BusinessCustomerService = mockBusinessCustomerService

    def authConnector = mockAuthConnector

    override def dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

}
