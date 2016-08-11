package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.{UKPassport, NonUKResidence, PersonResidenceType, ResponsiblePeople}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import play.api.test.Helpers._

import scala.concurrent.Future

class PersonResidentTypeControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new PersonResidentTypeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PersonResidentTypeController" must {

    "display person a UK resident page" in new Fixture {
      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("responsiblepeople.person.a.resident.title"))
    }

    "submit with valid Non UK data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isUKResidence" -> "true",
        "nino" -> "AA346464B",
        "countryOfBirth" -> "GB",
        "nationality" -> "GB"
      )

      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ContactDetailsController.get(1).url))
    }

    "Prepopulate UI with saved data" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(None,
        Some(PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("12346464646")),
        Country("United Kingdom", "GB"), Country("United Kingdom", "GB"))), None)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[name=ukPassportNumber]").`val`() must include("12346464646")
    }

    "fail submission on error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isUKResidence" -> "true",
        "nino" -> "AAAAAAAAA",
        "countryOfBirth" -> "GB",
        "nationality" -> ""
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#nationality]").html() must include(Messages("error.required.nationality"))
    }

    "submit with valid UK model data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isUKResidence" -> "true",
        "nino" -> "AA346464B",
        "countryOfBirth" -> "GB",
        "nationality" -> "GB"
      )

      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
    }
  }
}
