package controllers.aboutyou

import java.util.UUID

import connectors.DataCacheConnector
import forms.AreYouEmployedWithinTheBusinessForms._
import models.AreYouEmployedWithinTheBusinessModel
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

class AreYouEmployedWithinTheBusinessControllerSpec extends PlaySpec with OneServerPerSuite with Actions with MockitoSugar {

  implicit val request = FakeRequest()
  val userId = s"user-${UUID.randomUUID}"
  val areYouEmployedWithinTheBusinessModel = AreYouEmployedWithinTheBusinessModel(true)
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  override protected def authConnector = mockAuthConnector

  object MockAreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
    override protected def authConnector: AuthConnector = mockAuthConnector

    override def dataCacheConnector = mockDataCacheConnector
  }

  val fakePostRequest = FakeRequest("POST", "/about-you-2").withFormUrlEncodedBody("radio-inline" -> "true")
  //val loginDtls = AreYouEmployedWithinTheBusinessModel("testuser", "password")

  //For Loading the Page
  "On Page load" must {

    "load the Are You Employed Within the Business" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[AreYouEmployedWithinTheBusinessModel]
        (Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))
      val futureResult = MockAreYouEmployedWithinTheBusinessController.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("areYouEmployedWithinTheBusiness.lbl.title"))
    }

    "load Your Name page with pre populated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](Matchers.any()) 
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))
      val futureResult = MockAreYouEmployedWithinTheBusinessController.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("areYouEmployedWithinTheBusiness.lbl.title"))
    }

  }

  //For Submitting the Page
  "On Page submit" must {

    "navigate to the next page if Yes is supplied" in {
      submitWithYesOption { futureResult =>
        status(futureResult) must be(SEE_OTHER)
        contentAsString(futureResult) must include("") //TODO: Next Page content to be verified
      }
    }

    "navigate to the next page if No is supplied" in {
      submitWithNoOption { futureResult =>
        status(futureResult) must be(SEE_OTHER)
        contentAsString(futureResult) must include("") //TODO: Next Page content to be verified
      }
    }

  }

  def submitWithYesOption(futureResult: Future[Result] => Any) {
    createAreYouEmployedWithBusinessFormForSubmission(futureResult, "true")
  }

  def submitWithNoOption(futureResult: Future[Result] => Any) {
    createAreYouEmployedWithBusinessFormForSubmission(futureResult, "false")
  }

  def createAreYouEmployedWithBusinessFormForSubmission(futureResult: Future[Result] => Any, yesNo: String) {
    val areYouEmployedWithinTheBusinessModel = AreYouEmployedWithinTheBusinessModel(yesNo.toBoolean)
    val form = areYouEmployedWithinTheBusinessForm.fill(areYouEmployedWithinTheBusinessModel)
    val fakePostRequest = FakeRequest("POST", "/about-you-2").withFormUrlEncodedBody(form.data.toSeq: _*)
    when(mockDataCacheConnector.saveDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](Matchers.any(),
    Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))
    MockAreYouEmployedWithinTheBusinessController.post(mock[AuthContext], fakePostRequest)
  }

}
