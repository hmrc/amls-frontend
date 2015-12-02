package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.TelephoningBusiness
import org.mockito.Mockito._
import org.mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class TelephoningBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  private implicit val authContext = mock[AuthContext]
  private val mockAuthConnector = mock[AuthConnector]
  private val mockDataCacheConnector = mock[DataCacheConnector]
  private val EndpointURL = "/telephoning-business"
  private val InvalidNumber = "@@@@@@@@"
  private val ThirtyDigitNumber = "9" * 30
  private val ThirtyOneDigitNumber = "9" * 31

  object MockTelephoningBusinessController extends TelephoningBusinessController {
    def authConnector = mockAuthConnector

    override def dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  "On Page load" must {

    implicit val fakeGetRequest = FakeRequest()
    val telephoningBusiness = TelephoningBusiness(ThirtyDigitNumber, Some(ThirtyDigitNumber))

    "the blank Telephoning Business page if nothing in cache" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("telephoningbusiness.title"))
    }

    "the Business Telephone Number from the Cache" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusiness)))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(telephoningBusiness.businessPhoneNumber)

      //We should be able to parse the response back
      /*
        contentAsJson(futureResult) mustBe TelephoningBusiness
        contentAsJson(futureResult).data.size mustBe 1
      */
    }

    "the Mobile Number from the Cache" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusiness)))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(telephoningBusiness.mobileNumber match {
        case Some(mob) => mob
        case None => ""
      }
      )
    }

    "validate the contentType" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusiness)))
      val futureResult = MockTelephoningBusinessController.get
      contentType(futureResult) must be(Some("text/html"))
    }

    "validate the charset" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusiness)))
      val futureResult = MockTelephoningBusinessController.get
      charset(futureResult) mustEqual Some("utf-8")
    }

  }

  "On Page submit" must {

    "When the user does not provide Phone Number then throw Bad Request" in {
      val futureResult = telephoneBusinessFormSubmissionHelper("", Some(ThirtyDigitNumber))
      status(futureResult) must be(BAD_REQUEST)
      contentAsString(futureResult) must include(Messages("error.required"))
    }

    "When the user provides the Phone Number then validate the length" in {
      val futureResult = telephoneBusinessFormSubmissionHelper(ThirtyOneDigitNumber, Some(ThirtyDigitNumber))
      status(futureResult) must be(BAD_REQUEST)
      contentAsString(futureResult) must include(Messages("err.invalidLength"))
    }

    "When the user provides the Phone Number then validate the format" in {
      val futureResult = telephoneBusinessFormSubmissionHelper(InvalidNumber, Some(ThirtyDigitNumber))
      status(futureResult) must be(BAD_REQUEST)
      contentAsString(futureResult) must include(Messages("telephoningbusiness.error.invalidphonenumber"))
    }

    "When the user provides the Mobile Number then validate the length" in {
      val futureResult = telephoneBusinessFormSubmissionHelper(ThirtyDigitNumber, Some(ThirtyOneDigitNumber))
      status(futureResult) must be(BAD_REQUEST)
      contentAsString(futureResult) must include(Messages("err.invalidLength"))
    }

    "When the user provides the Mobile Number then validate the format" in {
      val futureResult = telephoneBusinessFormSubmissionHelper(InvalidNumber, Some(ThirtyOneDigitNumber))
      status(futureResult) must be(BAD_REQUEST)
      contentAsString(futureResult) must include(Messages("telephoningbusiness.error.invalidphonenumber"))
    }

    "Successfully navigate to the next page if the details are valid" in {
      val futureResult = telephoneBusinessFormSubmissionHelper(ThirtyDigitNumber, Some(ThirtyDigitNumber))
      status(futureResult) must be(SEE_OTHER)
      redirectLocation(futureResult).fold("") {identity} must include("/business-has-Email")
    }

  }

  private def telephoneBusinessFormSubmissionHelper(businessPhoneNumber: String, mobileNumber: Option[String]) = {
    val telephoningBusiness = TelephoningBusiness(businessPhoneNumber, mobileNumber)
    implicit val fakePostRequest = FakeRequest("POST", EndpointURL).withFormUrlEncodedBody(
      ("businessPhoneNumber", telephoningBusiness.businessPhoneNumber),
      ("mobileNumber", telephoningBusiness.mobileNumber.fold("") { x => x })
    )

    when(mockDataCacheConnector.saveDataShortLivedCache[TelephoningBusiness](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(telephoningBusiness)))
    MockTelephoningBusinessController.post

  }

}
