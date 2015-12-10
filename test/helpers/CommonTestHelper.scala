package helpers

import connectors.DataCacheConnector
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

object CommonTestHelper extends PlaySpec with MockitoSugar {

  def postAndVerifyResult[T](controllerMethod: (AuthContext, FakeRequest[AnyContentAsFormUrlEncoded]) => Future[Result],
                             model: T,
                             fm: Form[T],
                             mockDataCacheConnector: DataCacheConnector,
                             verifyResult: Future[Result] => Any) = {
    val tempForm = fm.fill(model)
    val request = FakeRequest("POST", "").withFormUrlEncodedBody(tempForm.data.toSeq: _*)
    val authContext = mock[AuthContext]

    when(mockDataCacheConnector.saveDataShortLivedCache[T](any(), any()) (any(), any(), any()))
      .thenReturn(Future.successful[Option[T]](Some(model)))

    verifyResult(controllerMethod(authContext, request))
  }

    def performVerify(expectedStatus: Int, expectedRedirectLocation: Option[String], expectedContent: String*): Future[Result] => Any = {
      result => {
        status(result) must be(expectedStatus)

        expectedRedirectLocation foreach {
          location => redirectLocation(result).fold("") {
            identity
          } must include(location)
        }

        expectedContent.foreach {
          contentAsString(result) must include(_)
        }
      }
    }
  }
