package controllers.declaration

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import utils.AuthorisedFixture

class AddPersonControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val addPersonController = new AddPersonController {
      override val dataCacheConnector = mockDataCacheConnector

      override protected def authConnector = self.authConnector
    }
  }

  "AddPersonController" must {

    "use the correct services" in new Fixture {
      addPersonController.dataCacheConnector must be(DataCacheConnector)
      addPersonController.authConnector must be(AMLSAuthConnector)
    }

  }


}
