package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
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

    "load 'not found' error page" when {
      "get throws an error " in new Fixture {
        val responsiblePeople = ResponsiblePeople()

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        val result = controller.get(10)(request)

        status(result) must be(NOT_FOUND)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title mustBe s"${Messages("error.not-found.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      }
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
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.NationalityController.get(1).url))
    }

    "show error with year field too long" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isUKResidence" -> "false",
        "dateOfBirth.day" -> "12",
        "dateOfBirth.month" -> "12",
        "dateOfBirth.year" -> "12345678",
        "passportType" -> "01",
        "ukPassportNumber" -> "12346464688",
        "countryOfBirth" -> "GB",
        "nationality" -> "GB"
      )
      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.expected.jodadate.format"))
    }

    "Prepopulate UI with saved data" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(
          None,
          Some(PersonResidenceType(
            NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("12346464646")),
            Country("United Kingdom", "GB"),
            Some(Country("United Kingdom", "GB")))
          ),
          None)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[name=ukPassportNumber]").`val`() must include("12346464646")
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

    "submit with valid UK model data" in new Fixture {

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

      val result = controller.post(1, false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.NationalityController.get(1).url))
    }

    "submit with valid UK data, transforming the NINO to uppercase" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isUKResidence" -> "true",
        "nino" -> "aa346464b",
        "countryOfBirth" -> "GB",
        "nationality" -> "GB"
      )

      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, edit = false)(newRequest)
      status(result) must be(SEE_OTHER)

      val captor = ArgumentCaptor.forClass(classOf[List[ResponsiblePeople]])
      verify(controller.dataCacheConnector).save(any(), captor.capture())(any(), any(), any())

      captor.getValue must have size 1

      (for {
        person <- captor.getValue.headOption
        residence <- person.personResidenceType
        nino <- residence.isUKResidence match {
          case UKResidence(n) => Some(n)
          case _ => None
        }
      } yield nino) map { _ mustBe "AA346464B" }

    }

    "submit with valid UK data, removing spaces and dashes" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isUKResidence" -> "true",
        "nino" -> "AA 34 64- 64 B",
        "countryOfBirth" -> "GB",
        "nationality" -> "GB"
      )

      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, edit = false)(newRequest)
      status(result) must be(SEE_OTHER)

      val captor = ArgumentCaptor.forClass(classOf[List[ResponsiblePeople]])
      verify(controller.dataCacheConnector).save(any(), captor.capture())(any(), any(), any())

      captor.getValue must have size 1

      (for {
        person <- captor.getValue.headOption
        residence <- person.personResidenceType
        nino <- residence.isUKResidence match {
          case UKResidence(n) => Some(n)
          case _ => None
        }
      } yield nino) map { _ mustBe "AA346464B" }

    }
    "throw error message when data is not valid" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nino" -> "AA346464B",
        "countryOfBirth" -> "GB",
        "nationality" -> "GB"
      )

      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, false)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#isUKResidence]").html() must include(Messages("error.required.rp.is.uk.resident"))
    }

    "redirect to 'not found' error page" when {
      "post throws exception" in new Fixture {

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

        val result = controller.post(10, false)(newRequest)
        status(result) must be(NOT_FOUND)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title mustBe s"${Messages("error.not-found.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      }
    }
  }
}
