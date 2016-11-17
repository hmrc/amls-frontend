package utils

import controllers.BaseController
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.Action
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import play.api.test.Helpers._

class FeatureToggleSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {



  trait TestController {
    self: BaseController =>
    def TestToggle:FeatureToggle
    lazy val get = TestToggle {
      Action {
        Ok("Success")
      }
    }
  }

  object ToggleOnController extends BaseController with TestController {
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val TestToggle = FeatureToggle(true)
  }

  object ToggleOffController extends BaseController with TestController {
    override protected val authConnector = mock[AuthConnector]
    override val TestToggle = FeatureToggle(false)
  }

  "ToggleOnController" must {
    "return the result of calling the inner action" in {
      val result = ToggleOnController.get(FakeRequest())
      status(result) mustBe 200
      contentAsString(result) mustBe "Success"
    }
  }

  "ToggleOffController" must {
    "return a `NotFound` result" in {
      val result = ToggleOffController.get(FakeRequest())
      status(result) mustBe NOT_FOUND
    }
  }
}
