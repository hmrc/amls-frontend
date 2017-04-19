package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{PersonName, ContactDetails, ResponsiblePeople}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ContactDetailsControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ContactDetailsController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ContactDetailsController" must {

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "on get display the contact details page" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

    }

    "on get display the contact details page with pre populated data" in new Fixture {

      val contact = ContactDetails("07702745869", "test@test.com")
      val res = ResponsiblePeople(personName = personName, contactDetails = Some(contact))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(res))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

    }

    "on post with valid data" when {
      "this is the first responsible person" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "07702745869",
          "emailAddress" -> "test@test.com"
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

        when(controller.dataCacheConnector.save[ContactDetails](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ConfirmAddressController.get(1).url))
      }

      "this is not the first responsible person" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "07702745869",
          "emailAddress" -> "test@test.com"
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(), ResponsiblePeople()))))

        when(controller.dataCacheConnector.save[ContactDetails](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(2)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CurrentAddressController.get(2).url))
      }
    }

     "fail submission on invalid phone number" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "<077>02745869",
        "emailAddress" -> "test@test.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      when(controller.dataCacheConnector.save[ContactDetails](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with missing phone data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "",
        "emailAddress" -> "test@test.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with missing email data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> ""
      )
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with invalid phone data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "invalid",
        "emailAddress" -> "test@test.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with invalid email data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> "invalid-email.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with greater than max length phone data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "1234567890123456789012345678901",
        "emailAddress" -> "test@test.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with greater than max length email data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> ("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz@" +
                           "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz.com")
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> "test@test.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

      when(controller.dataCacheConnector.save[ContactDetails](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
    }
  }
}


