package controllers.aboutyou

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.AboutYouForms._
import models.RoleForBusiness
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AmlsService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class RoleForBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"

  val mockAmlsService = mock[AmlsService]
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockRoleForBusinessController extends RoleForBusinessController {
    val authConnector = mockAuthConnector
    val amlsService: AmlsService = mockAmlsService
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "RoleForBusinessController" must {
    "use correct service" in {
      RoleForBusinessController.authConnector must be(AMLSAuthConnector)
    }

    "on load display the Role For Business page" in {
      implicit val request = FakeRequest()
      when(mockDataCacheConnector.fetchDataShortLivedCache[RoleForBusiness](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))
      val result = MockRoleForBusinessController.get(mock[AuthContext], request)
      status(result) must be(OK)
      contentAsString(result) must include("What is your role for the business?")
    }

    "prepopulate the Role For Business page" in {
      val aboutYou = RoleForBusiness("Other", "Cleaner")
      implicit val request = FakeRequest()
      when(mockDataCacheConnector.fetchDataShortLivedCache[RoleForBusiness](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))
      val result = MockRoleForBusinessController.get(mock[AuthContext], request)
      status(result) must be(OK)
      contentAsString(result) must include("What is your role for the business?")//TODO replace with pre-populated values
    }

    "on submit of valid role other than OTHER display the next page (currently NOT IMPLEMENTED)" in {
      val aboutYou = RoleForBusiness("01", "")
      val result = submitWithFormValues (aboutYou)
      status(result) must be(NOT_IMPLEMENTED)
    }

    "on submit without choosing a valid role re-display the page with validation error" in {
      val aboutYou = RoleForBusiness("", "")
      val result = submitWithFormValues (aboutYou)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("What is your role for the business?")
      contentAsString(result) must include(Messages("err.required"))
    }

    "on submit of valid role of OTHER with role entered in text field display the next page (currently NOT IMPLEMENTED)" in {
      val aboutYou = RoleForBusiness("Other", "Cleaner")
      val result = submitWithFormValues (aboutYou)
      status(result) must be(NOT_IMPLEMENTED)
    }

    "on submit of valid role of OTHER with NO role entered in text field re-display the page with validation error" in {
      val aboutYou = RoleForBusiness("02", "")
      val result = submitWithFormValues (aboutYou)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("What is your role for the business?")
      contentAsString(result) must include(Messages("err.required"))
    }

    def submitWithFormValues (aboutYou: RoleForBusiness) : Future[Result] = {
      val roleForBusinessForm1 = roleForBusinessForm.fill(aboutYou)
      implicit val request1 = FakeRequest("POST", "/role-for-business").withFormUrlEncodedBody( roleForBusinessForm1.data.toSeq : _*)
      when(mockDataCacheConnector.saveDataShortLivedCache[RoleForBusiness](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))
      val result = MockRoleForBusinessController.post(mock[AuthContext],request1)
      result
    }
  }
}
