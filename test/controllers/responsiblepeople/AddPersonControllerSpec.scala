package controllers.responsiblepeople

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople.{AddPerson, ResponsiblePeople}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class AddPersonControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

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

      when(addPersonController.dataCacheConnector.fetch[ResponsiblePeople](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = addPersonController.get(RecordId)(request)
      status(result) must be(OK)
    }

    "on get display the persons page with blank fields" in new Fixture {

      when(addPersonController.dataCacheConnector.fetch[ResponsiblePeople](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = addPersonController.get(RecordId)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=firstName]").`val` must be("")
      document.select("input[name=middleName]").`val` must be("")
      document.select("input[name=lastName]").`val` must be("")
    }

    "on get display the persons page with fields populated" in new Fixture {

      val addPerson = AddPerson("John", Some("Envy"), "Doe")
      val responsiblePeople = ResponsiblePeople(Some(addPerson))

      when(addPersonController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      val result = addPersonController.get(RecordId)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=firstName]").`val` must be("John")
      document.select("input[name=middleName]").`val` must be("Envy")
      document.select("input[name=lastName]").`val` must be("Doe")
    }

    "must pass on post with all the mandatory parameters supplied" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "firstName" -> "John",
        "lastName" -> "Doe"
      )

      when(addPersonController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = addPersonController.post(RecordId)(requestWithParams)
      status(result) must be(SEE_OTHER)
    }

    "must fail on post if first name not supplied" in new Fixture {

      val firstNameMissingInRequest = request.withFormUrlEncodedBody(
        "lastName" -> "Doe"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = addPersonController.post(RecordId)(firstNameMissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#firstName]").html() must include("This field is required")
    }

    "must fail on post if last name not supplied" in new Fixture {

      val lastNameMissingInRequest = request.withFormUrlEncodedBody(
        "firstName" -> "John"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = addPersonController.post(RecordId)(lastNameMissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#lastName]").html() must include("This field is required")
    }

  }

}
