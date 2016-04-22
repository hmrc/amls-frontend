package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople.{IsKnownByOtherNamesNo, PersonName, ResponsiblePeople}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class YourAnswersControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

    trait Fixture extends AuthorisedFixture {
      self =>

      val controller = new YourAnswersController {
        override val dataCacheConnector = mock[DataCacheConnector]
        override val authConnector = self.authConnector
      }
    }

    "Get" must {

      "use correct services" in new Fixture {
        YourAnswersController.authConnector must be(AMLSAuthConnector)
        YourAnswersController.dataCacheConnector must be(DataCacheConnector)
      }

      "load the your answers page when section data is available" in new Fixture {
        val model = ResponsiblePeople(None, None)
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))
        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include (Messages("responsiblepeople.your_answers.heading"))
      }

      "show the 'Add a responsible person' link" in new Fixture {

        val john = ResponsiblePeople(Some(PersonName("John", Some("Alan"), "Smith", IsKnownByOtherNamesNo)))
        val mark = ResponsiblePeople(Some(PersonName("Mark", None, "Smith", IsKnownByOtherNamesNo)))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(mark, john))))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include (Messages("responsiblepeople.check_your_answers.add"))

        val document = Jsoup.parse(contentAsString(result))
        println(document)
        document.getElementById("addResponsiblePerson").attr("href") must be (routes.PersonNameController.get(3).url)

      }

      "correctly display responsible people's full names" in new Fixture {

        val john = ResponsiblePeople(Some(PersonName("John", Some("Alan"), "Smith", IsKnownByOtherNamesNo)))
        val mark = ResponsiblePeople(Some(PersonName("Mark", None, "Smith", IsKnownByOtherNamesNo)))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(mark, john))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = contentAsString(result)
        document must include ("John Alan Smith")
        document must include ("Mark Smith")

      }

      "redirect to the main AMLS summary page when section data is unavailable" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      }
    }
  }
