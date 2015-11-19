package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.TelephoningBusiness
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class TelephoningBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  implicit val request = FakeRequest()
  implicit val authContext = mock[AuthContext]

  private val mockAuthConnector = mock[AuthConnector]
  private val mockDataCacheConnector = mock[DataCacheConnector]

  private val telephoningBusinessModel = TelephoningBusiness("12345678901", Some("12345678901"))

  object MockTelephoningBusinessController extends TelephoningBusinessController {
    override def authConnector = mockAuthConnector

    override def dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  "On Page load" must {

    "load the blank Telephoning Business page if nothing in cache" in {

      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentType(futureResult) must be(Some("text/html"))
      contentAsString(futureResult) must include(Messages("telephoningbusiness.title"))
    }

    "load the Business Telephone Number from the Cache" in {

      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusinessModel)))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentType(futureResult) must be(Some("text/html"))
      contentAsString(futureResult) must include(telephoningBusinessModel.businessPhoneNumber)
    }

    "load the Mobile Number from the Cache" in {

      when(mockDataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](Matchers.any())
      (Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(telephoningBusinessModel)))
      val futureResult = MockTelephoningBusinessController.get
      status(futureResult) must be(OK)
      contentType(futureResult) must be(Some("text/html"))
      contentAsString(futureResult) must include(telephoningBusinessModel.mobileNumber match {
        case Some(mob) => mob
        case None => ""
      }
      )
    }
  }


}
