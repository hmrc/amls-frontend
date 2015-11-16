package controllers.aboutyou

import java.util.UUID

import connectors.DataCacheConnector
import controllers.aboutYou.EmployedWithinTheBusinessController$$
import forms.EmployedWithinTheBusinessForms._
import models.EmployedWithinTheBusinessModel
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
  val userId = s"user-${UUID.randomUUID}"
  val areYouEmployedWithinTheBusinessModel = EmployedWithinTheBusinessModel(true)
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  override protected def authConnector = mockAuthConnector

  object MockEmployedWithinTheBusinessController$ extends EmployedWithinTheBusinessController$ {
    override protected def authConnector: AuthConnector = mockAuthConnector

    override def dataCacheConnector = mockDataCacheConnector
  }

  val fakePostRequest = FakeRequest("POST", "/about-you").withFormUrlEncodedBody("radio-inline" -> "true")
  //val loginDtls = AreYouEmployedWithinTheBusinessModel("testuser", "password")

  //For Loading the Page
  "On Page load" must {

    "load the Are You Employed Within the Business" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[EmployedWithinTheBusinessModel]
        (Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))
      val futureResult = MockEmployedWithinTheBusinessController$.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("areYouEmployedWithinTheBusiness.lbl.title"))
    }

    "load Are you employed with the Business without any Data " in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[EmployedWithinTheBusinessModel](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val futureResult = MockEmployedWithinTheBusinessController$.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("areYouEmployedWithinTheBusiness.lbl.title"))
    }

    "load Are you employed with the Business with pre populated data" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[EmployedWithinTheBusinessModel](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))
      val futureResult = MockEmployedWithinTheBusinessController$.get(mock[AuthContext], request)
      status(futureResult) must be(OK)
      contentAsString(futureResult) must include(Messages("areYouEmployedWithinTheBusiness.lbl.title"))
    }

  }

  //For Submitting the Page
  "On Page submit" must {

    "navigate to the next page if Yes is supplied" in {
      submitWithYesOption { futureResult =>
        status(futureResult) must be(SEE_OTHER)
        contentAsString(futureResult) must include(Messages("title.roleWithinBusiness"))
        //Redirect(controllers.aboutYou.routes.RoleWithinBusinessController.get())
      }
    }

    "navigate to the next page if No is supplied" in {
      submitWithNoOption { futureResult =>
        status(futureResult) must be(SEE_OTHER)
        contentAsString(futureResult) must include(Messages("title.roleForBusiness"))
      }
    }

    "stay on the page if errors are reported" in {
      submitWithoutAnOption { futureResult =>
        status(futureResult) must be(BAD_REQUEST)
        //contentAsString(futureResult) must include("") //TODO: Next Page content to be verified
      }
    }

  }

  def submitWithYesOption(futureResult: Future[Result] => Any) {
    createAreYouEmployedWithBusinessFormForSubmission(futureResult, "true")
  }

  def submitWithNoOption(futureResult: Future[Result] => Any) {
    createAreYouEmployedWithBusinessFormForSubmission(futureResult, "false")
  }

  def submitWithoutAnOption(futureResult: Future[Result] => Any) {
    createAreYouEmployedWithBusinessFormForSubmissionError(futureResult, "")
  }


  def createAreYouEmployedWithBusinessFormForSubmission(futureResult: Future[Result] => Any, isEmployed: String) {
    val areYouEmployedWithinTheBusinessModel = EmployedWithinTheBusinessModel(isEmployed.toBoolean)
    val fakePostRequest = FakeRequest("POST", "/about-you").withFormUrlEncodedBody(("isEmployed" , isEmployed))
    when(mockDataCacheConnector.saveDataShortLivedCache[EmployedWithinTheBusinessModel](Matchers.any(),
        Matchers.any()) (Matchers.any(), Matchers.any(), Matchers.any()))
       .thenReturn(Future.successful(Some(areYouEmployedWithinTheBusinessModel)))

    //dataCacheConnector.saveDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](CACHE_KEY_AREYOUEMPLOYED,
   //     areYouEmployedWithinTheBusinessModel)
    MockEmployedWithinTheBusinessController$.post(mock[AuthContext], fakePostRequest)
  }

  def createAreYouEmployedWithBusinessFormForSubmissionError(futureResult: Future[Result] => Any, isEmployed: String) {
    val fakePostRequest = FakeRequest("POST", "/about-you").withFormUrlEncodedBody(("isEmployed" , ""))
    MockEmployedWithinTheBusinessController$.post(mock[AuthContext], fakePostRequest)
  }

}
