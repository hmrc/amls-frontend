package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import helpers.CommonTestHelper._
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
import forms.AboutYouForms._

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

    "load Are You Employed Within the Business Page" in {
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
      postAndVerifyResult(
        MockEmployedWithinTheBusinessController.post(_, _),
        EmployedWithinTheBusiness(true),
        employedWithinTheBusinessForm,
        mockDataCacheConnector,
        performVerify(SEE_OTHER, Some("/role-within-business")))
    }

    "navigate to the next page if No is supplied" in {
      postAndVerifyResult(
        MockEmployedWithinTheBusinessController.post(_, _),
        EmployedWithinTheBusiness(false),
        employedWithinTheBusinessForm,
        mockDataCacheConnector,
        performVerify(SEE_OTHER, Some("/role-for-business")))
    }

    "stay on the page if errors are reported" in {
      val fakePostRequest = FakeRequest("POST", endpointURL).withFormUrlEncodedBody(("isEmployed", ""))
      status(MockEmployedWithinTheBusinessController.post(mock[AuthContext], fakePostRequest)) must be(BAD_REQUEST)
    }
  }

}
