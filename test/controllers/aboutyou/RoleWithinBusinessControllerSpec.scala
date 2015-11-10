package controllers

import java.util.UUID
import _root_.builders.AuthBuilder
import _root_.builders.SessionBuilder
import connectors.DataCacheConnector
import controllers.aboutYou.RoleWithinBusinessController
import models.{RoleWithinBusiness, LoginDetails}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AmlsService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import forms.AboutYouForms._
import org.mockito.Matchers._
import scala.concurrent.Future

class RoleWithinBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val mockAmlsService = mock[AmlsService]
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockRoleWithinBusinessController extends RoleWithinBusinessController {
    val authConnector = mockAuthConnector
    val amlsService: AmlsService = mockAmlsService
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "RoleWithinBusinessController" must {
    "on load display the Role Within Business page" in {
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
      when(mockDataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val result = MockRoleWithinBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
      status(result) must be(OK)
      contentAsString(result) must include("What is your role within the business?")
    }

    "on submit of valid role display the next page" in {
      val aboutYou = RoleWithinBusiness("Director")
      val roleWithinBusinessForm1 = roleWithinBusinessForm.fill(aboutYou)
      implicit val request1 = SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody( roleWithinBusinessForm1.data.toSeq : _*)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

      when(mockDataCacheConnector.saveDataShortLivedCache[RoleWithinBusiness](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))

      val result = MockRoleWithinBusinessController.onSubmit.apply(request1)
      status(result) must be(OK)
      contentAsString(result) must include("Check your answers")
    }

    "on submit without choosing a valid role re-display the page with validation error" in {
      val aboutYou = RoleWithinBusiness("")
      val roleWithinBusinessForm1 = roleWithinBusinessForm.fill(aboutYou)
      implicit val request1 = SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody( roleWithinBusinessForm1.data.toSeq : _*)
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

      when(mockDataCacheConnector.saveDataShortLivedCache[RoleWithinBusiness](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(aboutYou)))


      val result = MockRoleWithinBusinessController.onSubmit.apply(request1)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("What is your role within the business?")
      contentAsString(result) must include(Messages("error.required"))

    }

  }
}
