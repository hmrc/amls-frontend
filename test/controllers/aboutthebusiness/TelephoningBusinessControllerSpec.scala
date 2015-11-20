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
  private val telephoningBusinessModel = TelephoningBusiness("1111111111", Some("9999999999"))

  object MockTelephoningBusinessController extends TelephoningBusinessController {
    override def authConnector = mockAuthConnector

    override def dataCacheConnector = mockDataCacheConnector
  }

  "On Page load" must {

    implicit val fakeGetRequest = FakeRequest()

    "load the blank Telephoning Business page if nothing in cache" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("telephoningbusiness.title"))
    }

    "load the Business Telephone Number from the Cache" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusinessModel)))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(telephoningBusinessModel.businessPhoneNumber)
    }

    "load the Mobile Number from the Cache" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusinessModel)))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(telephoningBusinessModel.mobileNumber match {
        case Some(mob) => mob
        case None => ""
      }
      )
    }

    "validate the contentType" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusinessModel)))
      val futureResult = MockTelephoningBusinessController.get
      contentType(futureResult) must be(Some("text/html"))
    }

    "validate the charset" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusinessModel)))
      val futureResult = MockTelephoningBusinessController.get
      charset(futureResult) mustEqual Some("utf-8")
    }

  }


  "On Page submit" must {

    val endpointURL = "/telephoning-business"
    val telephoningBusinessMissingPhone = TelephoningBusiness("", Some("9999999999"))

    implicit val fakePostRequest = FakeRequest("POST", endpointURL).withFormUrlEncodedBody(
      ("businessPhoneNumber", telephoningBusinessMissingPhone.businessPhoneNumber),
      ("mobileNumber", telephoningBusinessMissingPhone.mobileNumber.get)
    )

    "stay on the Page when the user does not provide Phone Number" in {
      when(mockDataCacheConnector.saveDataShortLivedCache[TelephoningBusiness](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(telephoningBusinessMissingPhone)))
      val futureResult = MockTelephoningBusinessController.post
      status(futureResult) must be(BAD_REQUEST)
      contentAsString(futureResult) must include(Messages("error.required"))
    }

  }


}
