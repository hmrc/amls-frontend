package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{PersonName, ContactDetails, ResponsiblePeople}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ContactDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ContactDetailsController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ContactDetailsController" must {

    "on get display the contact details page" in new Fixture {
      when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("responsiblepeople.contact_details.title"))
    }


    "on get display the contact details page with pre populated data" in new Fixture {

      val contact = ContactDetails("07702745869", "test@test.com")
      val res = ResponsiblePeople(contactDetails = Some(contact))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(res))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=phoneNumber]").`val` must be("07702745869")
      document.select("input[name=emailAddress]").`val` must be("test@test.com")
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> "test@test.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[ContactDetails](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CurrentAddressController.get(1).url))
    }

    "on post with missing phone data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "",
        "emailAddress" -> "test@test.com"
      )

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(Messages("error.required.rp.phone"))
    }

    "on post with missing email data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> ""
      )

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(Messages("error.required.rp.email"))
    }

    "on post with invalid phone data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "invalid",
        "emailAddress" -> "test@test.com"
      )

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(Messages("error.invalid.rp.phone"))
    }

    "on post with invalid email data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> "invalid-email.com"
      )

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(Messages("error.invalid.rp.email"))
    }

    "on post with greater than max length phone data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "1234567890123456789012345678901",
        "emailAddress" -> "test@test.com"
      )

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#phoneNumber]").text() must include(Messages("error.max.length.rp.phone"))
    }

    "on post with greater than max length email data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> ("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz@" +
                           "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz.com")
      )

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#emailAddress]").text() must include(Messages("error.max.length.rp.email"))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "phoneNumber" -> "07702745869",
        "emailAddress" -> "test@test.com"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[ContactDetails](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SummaryController.get().url))
    }
  }
}


