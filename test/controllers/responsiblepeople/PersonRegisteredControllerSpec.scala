package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{PersonName, PreviousName, ResponsiblePeople}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{StatusConstants, AuthorisedFixture}

import scala.concurrent.Future

class PersonRegisteredControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new PersonRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PersonRegisteredController" when {

    "Get is called" must {

      "load the Person Registered page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("responsiblepeople.person.registered.title")} - ${Messages("progress.responsiblepeople.name")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        htmlValue.title mustBe title
      }

      "load the Person Registered page with a count of 0 when no responsible people have a name recorded" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(None,None), ResponsiblePeople(None,None)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("responsiblepeople.have.registered.person.text", 0))
      }

      "load the Person Registered page with a count of 0 when the responsible person has a status of deleted" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(status = Some(StatusConstants.Deleted))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("responsiblepeople.have.registered.person.text", 0))
      }

      "load the Person Registered page with a count of 2 when there are two complete responsible people" in new Fixture {
        val previousName = PreviousName(Some("first1"), Some("middle1"), Some("last1"), new LocalDate(1990, 2, 24))
        val personName = PersonName("first2", Some("middle2"), "last2", Some(previousName), Some("name"))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(Some(personName),None), ResponsiblePeople(Some(personName),None)))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("responsiblepeople.have.registered.people.text", 2))
      }
    }

    "Post is called" must {

      "successfully redirect to the page on selection of 'Yes'" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("registerAnotherPerson" -> "true")

        when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(false).url))
      }

      "successfully redirect to the page on selection of 'no'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("registerAnotherPerson" -> "false")

        when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CheckYourAnswersController.get().url))
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
      contentAsString(result) must include(Messages("responsiblepeople.want.to.register.another.person"))

    }
  }
}
