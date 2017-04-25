package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class NationalityControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new NationalityController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "NationalityController" must {
    val pageTitle = Messages("responsiblepeople.nationality.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "successfully load nationality page" in new Fixture {

      val responsiblePeople = ResponsiblePeople(personName)

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      val result = controller.get(1)(request)
      status(result) must be(OK)

      contentAsString(result) must include(Messages("responsiblepeople.nationality.title"))

      val document: Document = Jsoup.parse(contentAsString(result))
      document.title must be(pageTitle)
      document.select("input[type=radio][name=nationality][value=01]").hasAttr("checked") must be(false)
      document.select("input[type=radio][name=nationality][value=02]").hasAttr("checked") must be(false)
      document.select("input[type=radio][name=nationality][value=03]").hasAttr("checked") must be(false)
    }

    "successfully load Not found page" when {
      "get throws not found exception" in new Fixture {

        val responsiblePeople = ResponsiblePeople()

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        val result = controller.get(3)(request)
        status(result) must be(NOT_FOUND)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title mustBe s"${Messages("error.not-found.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

      }
    }

    "successfully load nationality page when nationality is none" in new Fixture {

      val pResidenceType = PersonResidenceType(UKResidence("AA346464B"), Country("United Kingdom", "GB"), None)
      val responsiblePeople = ResponsiblePeople(personName, Some(pResidenceType))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      val result = controller.get(1)(request)
      status(result) must be(OK)

      contentAsString(result) must include(Messages("responsiblepeople.nationality.title"))

      val document: Document = Jsoup.parse(contentAsString(result))
      document.title must be(pageTitle)
      document.select("input[type=radio][name=nationality][value=01]").hasAttr("checked") must be(false)
      document.select("input[type=radio][name=nationality][value=02]").hasAttr("checked") must be(false)
      document.select("input[type=radio][name=nationality][value=03]").hasAttr("checked") must be(false)
    }

    "successfully pre-populate UI with data from sav4later" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName,
        Some(PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("12346464646")),
          Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))), None)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[type=radio][name=nationality][value=01]").hasAttr("checked") must be(true)
      document.select("input[type=radio][name=nationality][value=02]").hasAttr("checked") must be(false)
      document.select("input[type=radio][name=nationality][value=03]").hasAttr("checked") must be(false)
    }

    "fail submission on error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title must be(pageTitle)
      document.select("a[href=#nationality]").html() must include(Messages("error.required.nationality"))
    }

    "successfully submit with valid nationality data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "01"
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

    "successfully submit with valid nationality data (with other country)" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "03",
        "otherCountry" -> "GB"
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

    "submit with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "02"
      )

      val pResidenceType = PersonResidenceType(UKResidence("AA346464B"), Country("United Kingdom", "GB"), None)
      val responsiblePeople = ResponsiblePeople(None, Some(pResidenceType))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      val prt = pResidenceType.copy(nationality = Some(Country("Ireland", "IE")))
      val responsiblePeople1 = ResponsiblePeople(None, Some(pResidenceType))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), meq(Seq(responsiblePeople1)))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
    }

    "load NotFound page on exception" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "02"
      )

      val responsiblePeople = ResponsiblePeople()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(10, true)(newRequest)
      status(result) must be(NOT_FOUND)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title mustBe s"${Messages("error.not-found.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
    }
  }
}
