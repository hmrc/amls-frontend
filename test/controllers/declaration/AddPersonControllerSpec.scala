package controllers.declaration

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.declaration.{AddPerson, Director}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
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

  val emptyCache = CacheMap("", Map.empty)

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

    "must pass on post with all the mandatory parameters supplied" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "firstName" -> "John",
        "lastName" -> "Doe",
        "roleWithinBusiness" -> "01"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = addPersonController.post()(requestWithParams)
      status(result) must be(SEE_OTHER)
    }

    "must fail on post if first name not supplied" in new Fixture {

      val firstNameMissingInRequest = request.withFormUrlEncodedBody(
        "lastName" -> "Doe",
        "roleWithinBusiness" -> "01"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = addPersonController.post()(firstNameMissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#firstName]").html() must include("This field is required")
    }

    "must fail on post if last name not supplied" in new Fixture {

      val lastNameNissingInRequest = request.withFormUrlEncodedBody(
        "firstName" -> "John",
        "roleWithinBusiness" -> "01"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = addPersonController.post()(lastNameNissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#lastName]").html() must include("This field is required")
    }

    "must fail on post if roleWithinBusiness not supplied" in new Fixture {

      val roleMissingInRequest = request.withFormUrlEncodedBody(
        "firstName" -> "John",
        "lastName" -> "Doe"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = addPersonController.post()(roleMissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#roleWithinBusiness]").html() must include("This field is required")

    }

  }

}
