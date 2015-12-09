package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.EmployedWithinTheBusiness
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}

import scala.concurrent.Future

class EmployedWithinTheBusinessControllerSpec extends PlaySpec with OneServerPerSuite with Actions with MockitoSugar {

  implicit val request = FakeRequest()
  private val employedWithinTheBusiness = EmployedWithinTheBusiness(true)
  private val mockAuthConnector = mock[AuthConnector]
  private val mockDataCacheConnector = mock[DataCacheConnector]
  private val endpointURL: String = "/employed-within-business"

  override protected def authConnector = mockAuthConnector

  object MockEmployedWithinTheBusinessController extends EmployedWithinTheBusinessController {
    override protected def authConnector: AuthConnector = mockAuthConnector

    override def dataCacheConnector = mockDataCacheConnector
  }

  "On Page load" must {
    "use correct service" in {
      EmployedWithinTheBusinessController.authConnector must be(AMLSAuthConnector)
    }

    "load the Are You Employed Within the Business Page" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[EmployedWithinTheBusiness]
        (Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(employedWithinTheBusiness)))
      val futureResult = MockEmployedWithinTheBusinessController.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("aboutyou.employedwithinbusiness.title"))
    }

    "load Are you employed with the Business without any Data " in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[EmployedWithinTheBusiness](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val futureResult = MockEmployedWithinTheBusinessController.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("aboutyou.employedwithinbusiness.title"))
    }

    "load Are you employed with the Business with pre populated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[EmployedWithinTheBusiness](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(employedWithinTheBusiness)))
      val futureResult = MockEmployedWithinTheBusinessController.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("aboutyou.employedwithinbusiness.title"))
    }
  }

  "On Page submit" must {
    "navigate to the next page if Yes is supplied" in {
      submitWithYesOption { futureResult =>
        status(futureResult) must be(SEE_OTHER)
        redirectLocation(futureResult).fold("") {identity} must include("/role-within-business")
      }
    }

    "navigate to the next page if No is supplied" in {
      submitWithNoOption { futureResult =>
        status(futureResult) must be(SEE_OTHER)
        redirectLocation(futureResult).fold("") {identity} must include("/role-for-business")
      }
    }

    "stay on the page if errors are reported" in {
      submitWithoutAnOption { futureResult =>
        status(futureResult) must be(BAD_REQUEST)
      }
    }
  }

  def submitWithYesOption(future: Future[Result] => Any) {
    employedWithBusinessFormForSubmission(future, "true")
  }

  def submitWithNoOption(future: Future[Result] => Any) {
    employedWithBusinessFormForSubmission(future, "false")
  }

  def submitWithoutAnOption(future: Future[Result] => Any) {
    employedWithBusinessFormForSubmissionError(future, "")
  }


  def employedWithBusinessFormForSubmission(future: Future[Result] => Any, isEmployed: String) {
    val employedWithinTheBusinessModel = EmployedWithinTheBusiness(isEmployed.toBoolean)
    val fakePostRequest = FakeRequest("POST", endpointURL).withFormUrlEncodedBody(("isEmployed", isEmployed))

    when(mockDataCacheConnector.saveDataShortLivedCache[EmployedWithinTheBusiness](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(employedWithinTheBusinessModel)))

    val result = MockEmployedWithinTheBusinessController.post(mock[AuthContext], fakePostRequest)
    future(result)
  }

  def employedWithBusinessFormForSubmissionError(futureResult: Future[Result] => Any, isEmployed: String) {
    val fakePostRequest = FakeRequest("POST", endpointURL).withFormUrlEncodedBody(("isEmployed", ""))
    MockEmployedWithinTheBusinessController.post(mock[AuthContext], fakePostRequest)
  }
}
