package controllers.supervision

import org.scalatestplus.play.PlaySpec
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class WhichProfessionalBodyControllerSpec extends PlaySpec with GenericTestHelper {

  trait Fixture extends DependencyMocks with AuthorisedFixture { self =>

    val request = addToken(authRequest)

    val controller = new WhichProfessionalBodyController(
      mockCacheConnector,
      self.authConnector
    )

  }

  "WhichProfessionalBodyControllerSpec" when {

    "get" must {

      "be called" in new Fixture {

        val result = controller.get()

      }
    }

    "post" must {

      "be called" in new Fixture {

        val result = controller.post()(request.withFormUrlEncodedBody())

      }

    }

  }

}
