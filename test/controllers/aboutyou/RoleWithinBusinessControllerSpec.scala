package controllers

import java.util.UUID
import _root_.builders.AuthBuilder
import _root_.builders.SessionBuilder
import connectors.DataCacheConnector
import models.LoginDetails
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AmlsService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class RoleWithinBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {
  implicit val request = FakeRequest()
  val userId = s"user-${UUID.randomUUID}"
  val mockAmlsService = mock[AmlsService]
  val mockAuthConnector = mock[AuthConnector]

  object MockRoleWithinBusinessController extends RoleWithinBusinessController {
    val authConnector = mockAuthConnector
    val amlsService: AmlsService = mockAmlsService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "RoleWithinBusinessController" must {
    "on load of page " must {
      "Authorised users" must {
        "load the Role Within Business page" in {
          implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
          AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          val result = MockRoleWithinBusinessController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
          status(result) must be(OK)
          contentAsString(result) must include("What is your role within the business?")
        }
      }
    }

    "on submit" must {
      "Authorised users" must {
        "successfully submit entered values" in {
          implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
          AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          val result = MockRoleWithinBusinessController.onSubmit.apply(SessionBuilder.buildRequestWithSession(userId))
          status(result) must be(OK)
          contentAsString(result) must include("Check your answers")
        }
      }
    }
  }
}

