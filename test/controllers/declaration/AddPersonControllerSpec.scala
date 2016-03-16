package controllers.declaration

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class AddPersonControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val addPersonController = new AddPersonController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  "AddPersonController" must {

    "use the correct services" in new Fixture {
      addPersonController.dataCacheConnector must be(DataCacheConnector)
      addPersonController.authConnector must be(AMLSAuthConnector)
    }

    "load the add person page with blank fields" in new Fixture {
      val result = addPersonController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("declaration.addperson.title"))
    }


  }


}
