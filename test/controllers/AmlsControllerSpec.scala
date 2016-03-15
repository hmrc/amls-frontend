package controllers

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class AmlsControllerSpec extends PlaySpec with OneServerPerSuite {

    trait UnauthenticatedFixture extends MockitoSugar {
      self =>
      implicit val unauthenticatedRequest = FakeRequest()
      val authConnector = mock[AuthConnector]
      val controller = new AmlsController {
        override protected def authConnector: AuthConnector = authConnector
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
