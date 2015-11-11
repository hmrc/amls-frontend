package controllers.aboutyou

import java.util.UUID
import _root_.builders.AuthBuilder
import _root_.builders.SessionBuilder
import com.sun.xml.internal.bind.v2.TODO
import connectors.DataCacheConnector
import controllers.aboutYou.RoleForBusinessController
import models.{RoleForBusiness}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.AmlsService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import forms.AboutYouForms._
import org.mockito.Matchers._
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
    "on load display the Role For Business page" in {
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchDataShortLivedCache[RoleForBusiness](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val result = MockRoleForBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) must be(OK)
      contentAsString(result) must include("What is your role for the business?")
    }


    "on submit of valid role other than OTHER display the next page (currently NOT IMPLEMENTED)" in {
      val aboutYou = RoleForBusiness("External accountant", "")
      val roleForBusinessForm1 = roleForBusinessForm.fill(aboutYou)
      implicit val request1 = SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody( roleForBusinessForm1.data.toSeq : _*)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

      when(mockDataCacheConnector.saveDataShortLivedCache[RoleForBusiness](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))

      val result = MockRoleForBusinessController.onSubmit.apply(request1)
      status(result) must be(NOT_IMPLEMENTED)
    }

    "on submit without choosing a valid role re-display the page with validation error" in {
      val aboutYou = RoleForBusiness("", "")
      val roleForBusinessForm1 = roleForBusinessForm.fill(aboutYou)
      implicit val request1 = SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody( roleForBusinessForm1.data.toSeq : _*)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

      when(mockDataCacheConnector.saveDataShortLivedCache[RoleForBusiness](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))

      val result = MockRoleForBusinessController.onSubmit.apply(request1)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("What is your role for the business?")
      contentAsString(result) must include(Messages("error.required"))
    }

    "on submit of valid role of OTHER with role entered in text field display the next page (currently NOT IMPLEMENTED)" in {
      val aboutYou = RoleForBusiness("Other", "Cleaner")
      val roleForBusinessForm1 = roleForBusinessForm.fill(aboutYou)
      implicit val request1 = SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody( roleForBusinessForm1.data.toSeq : _*)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

      when(mockDataCacheConnector.saveDataShortLivedCache[RoleForBusiness](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))

      val result = MockRoleForBusinessController.onSubmit.apply(request1)
      status(result) must be(NOT_IMPLEMENTED)
    }

    "on submit of valid role of OTHER with NO role entered re-display the page with validation error" in {
      val aboutYou = RoleForBusiness("Other", "")
      val roleForBusinessForm1 = roleForBusinessForm.fill(aboutYou)
      implicit val request1 = SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody( roleForBusinessForm1.data.toSeq : _*)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

      when(mockDataCacheConnector.saveDataShortLivedCache[RoleForBusiness](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))

      val result = MockRoleForBusinessController.onSubmit.apply(request1)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("What is your role for the business?")
      contentAsString(result) must include(Messages("error.required"))
    }

  }
}
