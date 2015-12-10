package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.aboutyou.RoleWithinBusinessController
import forms.AboutYouForms._
import models.RoleWithinBusiness
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import helpers.CommonTestHelper._
import scala.concurrent.Future

class RoleWithinBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object MockRoleWithinBusinessController extends RoleWithinBusinessController {
    val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "RoleWithinBusinessController" must {
    "use correct service" in {
      RoleWithinBusinessController.authConnector must be(AMLSAuthConnector)
    }

    "on load display the Role Within Business page" in {
      when(mockDataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](any())
        (any(), any(),  any())).thenReturn(Future.successful(None))
      val result = MockRoleWithinBusinessController.get(mock[AuthContext], request)
      status(result) must be(OK)
      contentAsString(result) must include("What is your role within the business?")
    }
    "on load display the Role Within Business page with pre populated data" in {
      val roleWithinBusiness = RoleWithinBusiness("07", "Cleaner")
      when(mockDataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](any())
        (any(), any(),  any())).thenReturn(Future.successful(Some(roleWithinBusiness)))
      val result = MockRoleWithinBusinessController.get(mock[AuthContext], request)
      status(result) must be(OK)
      contentAsString(result) must include("What is your role within the business?")
    }

    "on submit of valid role other than OTHER display the next page (currently NOT IMPLEMENTED)" in {
      postAndTestResult( MockRoleWithinBusinessController.post(_,_),
        RoleWithinBusiness("01", ""), roleWithinBusinessForm, mockDataCacheConnector, verifyResult(SEE_OTHER))
    }

    "on submit without choosing a valid role re-display the page with validation error" in {
      postAndTestResult( MockRoleWithinBusinessController.post(_,_),
        RoleWithinBusiness("", ""), roleWithinBusinessForm, mockDataCacheConnector, verifyResult(BAD_REQUEST, "What is your role within the business?", Messages("err.required")))
    }

    "on submit of valid role of OTHER with role entered in text field display the next page (currently NOT IMPLEMENTED)" in {
      postAndTestResult( MockRoleWithinBusinessController.post(_,_),
        RoleWithinBusiness("07", "Cleaner"), roleWithinBusinessForm, mockDataCacheConnector, verifyResult(SEE_OTHER))
    }

    "on submit of valid role of OTHER with NO role entered in text field re-display the page with validation error" in {
      postAndTestResult( MockRoleWithinBusinessController.post(_,_),
        RoleWithinBusiness("07", ""), roleWithinBusinessForm, mockDataCacheConnector, verifyResult(BAD_REQUEST, "What is your role within the business?", Messages("err.required")))
    }
  }
}
