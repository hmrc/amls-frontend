package controllers.declaration

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.declaration.{AddPerson, Director}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

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
      AddPersonController.dataCacheConnector must be(DataCacheConnector)
      AddPersonController.authConnector must be(AMLSAuthConnector)
    }

    "on get display the persons page" in new Fixture {

      when(addPersonController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = addPersonController.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("declaration.addperson.title"))
    }

    "on get display the persons page with blank fields" in new Fixture {

      when(addPersonController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = addPersonController.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=firstName]").`val` must be("")
      document.select("input[name=middleName]").`val` must be("")
      document.select("input[name=lastName]").`val` must be("")
      document.select("input[name=roleWithinBusiness][checked]").`val` must be("")
    }

    "on get display the persons page with fields populated" in new Fixture {

      val addPerson = AddPerson("John", Some("Envy"),
        "Doe", Director)

      when(addPersonController.dataCacheConnector.fetch[AddPerson](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(addPerson)))

      val result = addPersonController.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=firstName]").`val` must be("John")
      document.select("input[name=middleName]").`val` must be("Envy")
      document.select("input[name=lastName]").`val` must be("Doe")
      document.select("input[name=roleWithinBusiness][checked]").`val` must be("02")
    }

  }

}
