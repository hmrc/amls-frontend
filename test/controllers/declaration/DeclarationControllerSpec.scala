package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.declaration.{InternalAccountant, AddPerson}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import utils.AuthorisedFixture
import play.api.test.Helpers._

import scala.concurrent.Future

class DeclarationControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val declarationController = new DeclarationController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
    }

  }

  "Declaration get" must {

    "use the correct services" in new Fixture {
      DeclarationController.authConnector must be(AMLSAuthConnector)
      DeclarationController.dataCacheConnector must be(DataCacheConnector)
    }

    "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {
      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = declarationController.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
    }

    "load the declaration page if name and business matching is found" in new Fixture {

      val addPerson = AddPerson("John", Some("Envy"), "Doe", InternalAccountant)

      when(declarationController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = declarationController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(addPerson.firstName)
      contentAsString(result) must include(addPerson.middleName mkString)
      contentAsString(result) must include(addPerson.lastName)
    }


  }

}
