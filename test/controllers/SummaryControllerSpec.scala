package controllers

import java.util.UUID
import builders.{AuthBuilder, SessionBuilder}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AmlsService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class SummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  val userId = s"user-${UUID.randomUUID}"
  val mockAmlsService = mock[AmlsService]
  val mockAuthConnector = mock[AuthConnector]

  val mainSummaryController = new MainSummaryController {
    val authConnector = mockAuthConnector
    val amlsService: AmlsService = mockAmlsService
  }

  "MainSummaryController" must {
        "display a page" in {
          implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
          AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          val result = mainSummaryController.onPageLoad.apply(SessionBuilder.buildRequestWithSession(userId))
          status(result) must be(OK)
      }
    }
}

