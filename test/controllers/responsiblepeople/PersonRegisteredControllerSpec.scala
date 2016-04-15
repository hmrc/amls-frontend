package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.ResponsiblePeople
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class PersonRegisteredControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new PersonRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PersonRegisteredController" must {

    "Get Option:" must {

      "load the Person Registered page" in new Fixture {

        when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("responsiblepeople.person.registered.title")
      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes'" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("registerAnotherPerson" -> "true")

        when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.AddPersonController.get(2).url))
      }

      "successfully redirect to the page on selection of 'no'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("registerAnotherPerson" -> "false")

        when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SummaryController.get().url))
      }
    }

    "on post invalid data show error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("responsiblepeople.want.to.register.another.person"))

    }

    "on post with invalid data show error" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "registerAnotherPerson" -> ""
      )
      when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("err.summary"))

    }
  }
}
