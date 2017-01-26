package controllers

import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.GenericTestHelper

class AmlsControllerSpec extends GenericTestHelper {

    trait UnauthenticatedFixture extends MockitoSugar {
      self =>
      implicit val unauthenticatedRequest = FakeRequest()
      val mockAuthConnector = mock[AuthConnector]
      val controller = new AmlsController {
        override protected def authConnector: AuthConnector = mockAuthConnector
      }
    }

    "AmlsController" must {
      "load the unauthorised page with an unauthenticated request" in new UnauthenticatedFixture {
          val result = controller.unauthorised(unauthenticatedRequest)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("unauthorised.title"))
        }
    }
}
