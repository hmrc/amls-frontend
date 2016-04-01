package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class RegisteredForSelfAssessmentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RegisteredForSelfAssessmentController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }
  "RegisteredForSelfAssessmentController" must {

      "use correct services" in new Fixture {
        RegisteredForSelfAssessmentController.authConnector must be(AMLSAuthConnector)
        RegisteredForSelfAssessmentController.dataCacheConnector must be(DataCacheConnector)
      }

    "get" must {

      "load the page" in new Fixture {
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("responsiblepeople.registeredforselfassessment.title"))
      }
    }
  }
}
